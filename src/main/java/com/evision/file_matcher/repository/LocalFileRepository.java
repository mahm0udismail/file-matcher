package com.evision.file_matcher.repository;

import com.evision.file_matcher.exception.FileProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class LocalFileRepository implements FileRepository {

    private static final Pattern WORD_PATTERN = Pattern.compile("[a-zA-Z]+");

    /**
     * Streams a file line by line, extracts only alphabetic tokens,
     * lowercases them, and returns the unique word set.
     * <p>
     * Performance:
     * - BufferedReader reads one line at a time — constant memory regardless of file size.
     * - Pre-compiled Pattern eliminates per-line regex compilation.
     * - Matcher scans each line in-place — no intermediate String[] allocated.
     * - HashSet stores unique words only (~50K–100K entries for natural language files).
     *
     * @param filePath path to the file to tokenize
     * @return set of unique lowercase alphabetic words found in the file
     * @throws FileProcessingException if the file cannot be read
     */
    @Override
    public Set<String> loadWords(Path filePath) {
        log.debug("Loading words from file: {}", filePath);

        Set<String> words = new HashSet<>();

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {

            String line;
            while ((line = reader.readLine()) != null) {
                extractWords(line, words);
            }

        } catch (IOException e) {
            throw new FileProcessingException("Failed to read file: " + filePath, e);
        }

        log.debug("Loaded {} unique words from: {}", words.size(), filePath);
        return words;
    }

    /**
     * Lists all regular files directly inside the given directory.
     * Does not recurse into subdirectories.
     *
     * @param directoryPath path to the pool directory
     * @return list of paths to regular files in the directory
     * @throws FileProcessingException if the directory cannot be read
     */
    @Override
    public List<Path> listPoolFiles(Path directoryPath) {
        log.debug("Listing pool files in directory: {}", directoryPath);

        try (var stream = Files.list(directoryPath)) {

            List<Path> files = stream.filter(Files::isRegularFile).collect(Collectors.toList());

            log.debug("Found {} pool file(s) in: {}", files.size(), directoryPath);
            return files;

        } catch (IOException e) {
            throw new FileProcessingException("Failed to list files in directory: " + directoryPath, e);
        }
    }

    /**
     * Uses a pre-compiled Matcher to scan one line in-place.
     * <p>
     * Each call to matcher.find() advances the internal index —
     * no array is created, no substrings are allocated until matcher.group()
     * is called, and only for tokens we actually want to keep.
     *
     * @param line  one line of text from the file
     * @param words the accumulating set to add valid words into
     */
    private void extractWords(String line, Set<String> words) {
        Matcher matcher = WORD_PATTERN.matcher(line);

        while (matcher.find()) {
            words.add(matcher.group().toLowerCase());
        }
    }
}