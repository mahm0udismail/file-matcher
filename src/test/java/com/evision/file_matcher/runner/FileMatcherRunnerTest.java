package com.evision.file_matcher.runner;

import com.evision.file_matcher.model.SimilarityResult;
import com.evision.file_matcher.service.FileMatcherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@DisplayName("FileMatcherRunner")
@ExtendWith(MockitoExtension.class)
class FileMatcherRunnerTest {

    @Mock
    private FileMatcherService fileMatcherService;

    @Mock
    private ApplicationArguments applicationArguments;

    @InjectMocks
    private FileMatcherRunner fileMatcherRunner;

    // Happy path

    @Test
    @DisplayName("Should print all results to stdout when matches are found")
    void shouldPrintAllResultsToStdout() throws Exception {
        // Arrange
        List<SimilarityResult> results = List.of(
                new SimilarityResult("document_c.txt", 87.50),
                new SimilarityResult("document_a.txt", 63.20),
                new SimilarityResult("document_b.txt", 12.00)
        );
        when(fileMatcherService.findMatches()).thenReturn(results);

        // Capture stdout
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            // Act
            fileMatcherRunner.run(applicationArguments);

            // Assert
            String output = outputStream.toString();
            assertThat(output).contains("document_c.txt");
            assertThat(output).contains("document_a.txt");
            assertThat(output).contains("document_b.txt");
            assertThat(output).contains("87.50");
            assertThat(output).contains("63.20");
            assertThat(output).contains("12.00");

        } finally {
            // Always restore stdout — even if assertions fail
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("Should print best match line to stdout")
    void shouldPrintBestMatchToStdout() throws Exception {
        // Arrange
        List<SimilarityResult> results = List.of(
                new SimilarityResult("best_file.txt", 95.00),
                new SimilarityResult("other_file.txt", 40.00)
        );
        when(fileMatcherService.findMatches()).thenReturn(results);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            // Act
            fileMatcherRunner.run(applicationArguments);

            // Assert
            String output = outputStream.toString();
            assertThat(output).contains("Best match");
            assertThat(output).contains("best_file.txt");
            assertThat(output).contains("95.00");

        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("Should print empty message when pool has no files")
    void shouldPrintEmptyMessage_whenNoPoolFilesFound() throws Exception {
        // Arrange
        when(fileMatcherService.findMatches()).thenReturn(List.of());

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outputStream));

        try {
            // Act
            fileMatcherRunner.run(applicationArguments);

            // Assert
            String output = outputStream.toString();
            assertThat(output).contains("No pool files found to compare");

        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    @DisplayName("Should call findMatches exactly once")
    void shouldCallFindMatchesExactlyOnce() throws Exception {
        // Arrange
        when(fileMatcherService.findMatches()).thenReturn(List.of(
                new SimilarityResult("file.txt", 75.0)
        ));

        // Act
        fileMatcherRunner.run(applicationArguments);

        // Assert
        verify(fileMatcherService, times(1)).findMatches();
    }

}