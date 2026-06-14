package com.evision.file_matcher.service;

import com.evision.file_matcher.config.FileMatcherProperties;
import com.evision.file_matcher.exception.FileProcessingException;
import com.evision.file_matcher.exception.InvalidConfigurationException;
import com.evision.file_matcher.model.SimilarityResult;
import com.evision.file_matcher.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Orchestrates the file similarity matching workflow.
 * <p>
 * Workflow:
 * 1. Validate reference file and pool directory paths.
 * 2. Load the reference file word set once.
 * 3. Process each pool file in parallel (tokenize + score).
 * 4. Return results sorted by score descending.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileMatcherService {

    private final FileRepository fileRepository;
    private final SimilarityCalculator similarityCalculator;
    private final FileMatcherProperties properties;

    /**
     * Runs the full matching workflow and returns ranked similarity results.
     *
     * @return list of {@link SimilarityResult} sorted by score descending
     * @throws InvalidConfigurationException if paths are invalid
     * @throws FileProcessingException       if any file cannot be read
     */
    public List<SimilarityResult> findMatches() {

        Path referenceFile = resolveReferenceFile();
        Path poolDirectory = resolvePoolDirectory();

        log.info("Reference file : {}", referenceFile);
        log.info("Pool directory : {}", poolDirectory);

        // Load File A once — shared across all comparisons (read-only, thread-safe)
        Set<String> referenceWords = fileRepository.loadWords(referenceFile);
        log.info("Reference file loaded — {} unique word(s)", referenceWords.size());

        List<Path> poolFiles = fileRepository.listPoolFiles(poolDirectory);
        log.info("Pool contains {} file(s)", poolFiles.size());

        if (poolFiles.isEmpty()) {
            log.warn("No files found in pool directory: {}", poolDirectory);
            return List.of();
        }

        return processPoolFiles(referenceWords, poolFiles);
    }

    /**
     * Resolves and validates the reference file path.
     *
     * @return validated {@link Path} to the reference file
     * @throws InvalidConfigurationException if the path does not point to a regular file
     */
    private Path resolveReferenceFile() {
        Path path = Paths.get(properties.referenceFile());

        if (!Files.exists(path)) {
            throw new InvalidConfigurationException("Reference file does not exist: " + path);
        }
        if (!Files.isRegularFile(path)) {
            throw new InvalidConfigurationException("Reference file path is not a regular file: " + path);
        }
        return path;
    }

    /**
     * Resolves and validates the pool directory path.
     *
     * @return validated {@link Path} to the pool directory
     * @throws InvalidConfigurationException if the path does not point to a directory
     */
    private Path resolvePoolDirectory() {
        Path path = Paths.get(properties.poolDirectory());

        if (!Files.exists(path)) {
            throw new InvalidConfigurationException("Pool directory does not exist: " + path);
        }
        if (!Files.isDirectory(path)) {
            throw new InvalidConfigurationException("Pool directory path is not a directory: " + path);
        }
        return path;
    }

    /**
     * Processes all pool files in parallel using a bounded thread pool.
     * <p>
     * ExecutorService is used in try-with-resources (Java 19+, AutoCloseable)
     * to guarantee shutdown is called even if an exception is thrown.
     * <p>
     * Thread pool size is capped at the smaller of pool size and available
     * processors — avoids creating unnecessary threads on small machines.
     *
     * @param referenceWords word set from File A
     * @param poolFiles      list of pool file paths
     * @return list of {@link SimilarityResult} sorted by score descending
     */
    private List<SimilarityResult> processPoolFiles(Set<String> referenceWords, List<Path> poolFiles) {
        int threadCount = Math.min(poolFiles.size(), Runtime.getRuntime().availableProcessors());
        log.debug("Processing {} pool file(s) with {} thread(s)", poolFiles.size(), threadCount);

        // try-with-resources guarantees executor.close() → shutdown() on exit
        try (ExecutorService executor = Executors.newFixedThreadPool(threadCount)) {

            List<CompletableFuture<SimilarityResult>> futures = poolFiles.stream()
                    .map(poolFile -> CompletableFuture.supplyAsync(
                            () -> scoreFile(referenceWords, poolFile), executor)
                    )
                    .toList();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .sorted(Comparator.comparingDouble(SimilarityResult::score).reversed())
                    .toList();
        }
    }

    /**
     * Tokenizes a single pool file and computes its similarity score
     * against the reference word set.
     * <p>
     * Designed to run inside a CompletableFuture — no shared mutable state.
     *
     * @param referenceWords word set from File A (read-only)
     * @param poolFile       path to the pool file being scored
     * @return {@link SimilarityResult} for this pool file
     */
    private SimilarityResult scoreFile(Set<String> referenceWords, Path poolFile) {
        log.debug("Scoring file: {}", poolFile.getFileName());

        Set<String> candidateWords = fileRepository.loadWords(poolFile);
        double score = similarityCalculator.calculate(referenceWords, candidateWords);

        // Format score before passing to SLF4J — plain {} placeholders only
        log.debug("Score for {} : {}%", poolFile.getFileName(), String.format("%.2f", score));

        return new SimilarityResult(poolFile.getFileName().toString(), score);
    }
}