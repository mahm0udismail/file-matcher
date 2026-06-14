package com.evision.file_matcher.runner;

import com.evision.file_matcher.exception.FileProcessingException;
import com.evision.file_matcher.exception.InvalidConfigurationException;
import com.evision.file_matcher.model.SimilarityResult;
import com.evision.file_matcher.service.FileMatcherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Application entry point — executes after the Spring context is fully loaded.
 * <p>
 * Responsibilities:
 * - Trigger the matching workflow via {@link FileMatcherService}.
 * - Print ranked similarity results to stdout.
 * - Handle and report errors cleanly without exposing stack traces to the user.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileMatcherRunner implements ApplicationRunner {

    private final FileMatcherService fileMatcherService;

    /**
     * @param args application arguments provided at startup (not used by this application)
     */
    @Override
    public void run(@NonNull ApplicationArguments args) {
        try {
            List<SimilarityResult> results = fileMatcherService.findMatches();
            printResults(results);

        } catch (InvalidConfigurationException e) {
            log.error("Configuration error: {}", e.getMessage());
            System.err.println("Configuration error: " + e.getMessage());
            System.exit(1);

        } catch (FileProcessingException e) {
            log.error("File processing error: {}", e.getMessage());
            System.err.println("File processing error: " + e.getMessage());
            System.exit(1);
        }
    }
    
    /**
     * Prints the similarity results to stdout in a clean, readable format.
     * Results are expected to arrive pre-sorted by score descending
     * from the service layer.
     *
     * @param results list of similarity results sorted by score descending
     */
    private void printResults(List<SimilarityResult> results) {
        if (results.isEmpty()) {
            System.out.println("No pool files found to compare.");
            return;
        }

        System.out.println("=".repeat(50));
        System.out.println(" File Similarity Results");
        System.out.println("=".repeat(50));

        for (int i = 0; i < results.size(); i++) {
            SimilarityResult result = results.get(i);
            System.out.printf(" %d. %-30s %.2f%%%n", i + 1, result.fileName(), result.score());
        }

        System.out.println("=".repeat(50));

        SimilarityResult best = results.getFirst();
        System.out.printf(" Best match : %s (%.2f%%)%n", best.fileName(), best.score());

        System.out.println("=".repeat(50));
    }
}