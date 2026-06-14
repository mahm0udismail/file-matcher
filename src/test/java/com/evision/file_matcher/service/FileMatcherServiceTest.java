package com.evision.file_matcher.service;

import com.evision.file_matcher.config.FileMatcherProperties;
import com.evision.file_matcher.exception.InvalidConfigurationException;
import com.evision.file_matcher.model.SimilarityResult;
import com.evision.file_matcher.repository.FileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@DisplayName("FileMatcherService")
@ExtendWith(MockitoExtension.class)
class FileMatcherServiceTest {

    @TempDir
    Path tempDir;

    @Mock
    private FileRepository fileRepository;

    @Mock
    private SimilarityCalculator similarityCalculator;

    @Mock
    private FileMatcherProperties properties;

    @InjectMocks
    private FileMatcherService fileMatcherService;

    // Happy path

    @Test
    @DisplayName("Should return results sorted by score descending")
    void shouldReturnResultsSortedByScoreDescending() throws IOException {
        // Arrange
        Path referenceFile = createFile("fileA.txt");
        Path poolDir = Files.createDirectory(tempDir.resolve("pool"));
        Path poolFile1 = createFileIn(poolDir, "file1.txt");
        Path poolFile2 = createFileIn(poolDir, "file2.txt");
        Path poolFile3 = createFileIn(poolDir, "file3.txt");

        Set<String> referenceWords = Set.of("the", "quick", "brown", "fox");
        Set<String> wordsFile1 = Set.of("the", "quick");
        Set<String> wordsFile2 = Set.of("the", "quick", "brown", "fox");
        Set<String> wordsFile3 = Set.of("hello");

        when(properties.referenceFile()).thenReturn(referenceFile.toString());
        when(properties.poolDirectory()).thenReturn(poolDir.toString());

        when(fileRepository.loadWords(referenceFile)).thenReturn(referenceWords);
        when(fileRepository.loadWords(poolFile1)).thenReturn(wordsFile1);
        when(fileRepository.loadWords(poolFile2)).thenReturn(wordsFile2);
        when(fileRepository.loadWords(poolFile3)).thenReturn(wordsFile3);

        when(fileRepository.listPoolFiles(poolDir)).thenReturn(List.of(poolFile1, poolFile2, poolFile3));

        when(similarityCalculator.calculate(referenceWords, wordsFile1)).thenReturn(50.0);
        when(similarityCalculator.calculate(referenceWords, wordsFile2)).thenReturn(100.0);
        when(similarityCalculator.calculate(referenceWords, wordsFile3)).thenReturn(0.0);

        // Act
        List<SimilarityResult> results = fileMatcherService.findMatches();

        // Assert — sorted descending by score
        assertThat(results).hasSize(3);
        assertThat(results.get(0).score()).isEqualTo(100.0);
        assertThat(results.get(1).score()).isEqualTo(50.0);
        assertThat(results.get(2).score()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Should load reference file words exactly once")
    void shouldLoadReferenceFileWordsExactlyOnce() throws IOException {
        // Arrange
        Path referenceFile = createFile("fileA.txt");
        Path poolDir = Files.createDirectory(tempDir.resolve("pool"));
        Path poolFile1 = createFileIn(poolDir, "file1.txt");
        Path poolFile2 = createFileIn(poolDir, "file2.txt");

        when(properties.referenceFile()).thenReturn(referenceFile.toString());
        when(properties.poolDirectory()).thenReturn(poolDir.toString());

        when(fileRepository.loadWords(referenceFile)).thenReturn(Set.of("hello"));
        when(fileRepository.loadWords(poolFile1)).thenReturn(Set.of("hello"));
        when(fileRepository.loadWords(poolFile2)).thenReturn(Set.of("world"));

        when(fileRepository.listPoolFiles(poolDir)).thenReturn(List.of(poolFile1, poolFile2));

        when(similarityCalculator.calculate(any(), any())).thenReturn(50.0);

        // Act
        fileMatcherService.findMatches();

        // Assert — reference file loaded exactly once regardless of pool size
        verify(fileRepository, times(1)).loadWords(referenceFile);
    }

    @Test
    @DisplayName("Should score every pool file exactly once")
    void shouldScoreEveryPoolFileExactlyOnce() throws IOException {
        // Arrange
        Path referenceFile = createFile("fileA.txt");
        Path poolDir = Files.createDirectory(tempDir.resolve("pool"));
        Path poolFile1 = createFileIn(poolDir, "file1.txt");
        Path poolFile2 = createFileIn(poolDir, "file2.txt");

        when(properties.referenceFile()).thenReturn(referenceFile.toString());
        when(properties.poolDirectory()).thenReturn(poolDir.toString());

        when(fileRepository.loadWords(referenceFile)).thenReturn(Set.of("hello"));
        when(fileRepository.loadWords(poolFile1)).thenReturn(Set.of("hello"));
        when(fileRepository.loadWords(poolFile2)).thenReturn(Set.of("world"));

        when(fileRepository.listPoolFiles(poolDir)).thenReturn(List.of(poolFile1, poolFile2));

        when(similarityCalculator.calculate(any(), any())).thenReturn(50.0);

        // Act
        fileMatcherService.findMatches();

        // Assert — each pool file tokenized exactly once
        verify(fileRepository, times(1)).loadWords(poolFile1);
        verify(fileRepository, times(1)).loadWords(poolFile2);
    }

    @Test
    @DisplayName("Should return empty list when pool directory is empty")
    void shouldReturnEmptyList_whenPoolDirectoryIsEmpty() throws IOException {
        // Arrange
        Path referenceFile = createFile("fileA.txt");
        Path poolDir = Files.createDirectory(tempDir.resolve("pool"));

        when(properties.referenceFile()).thenReturn(referenceFile.toString());
        when(properties.poolDirectory()).thenReturn(poolDir.toString());

        when(fileRepository.loadWords(referenceFile)).thenReturn(Set.of("hello"));
        when(fileRepository.listPoolFiles(poolDir)).thenReturn(List.of());

        // Act
        List<SimilarityResult> results = fileMatcherService.findMatches();

        // Assert
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Should return single result when pool has one file")
    void shouldReturnSingleResult_whenPoolHasOneFile() throws IOException {
        // Arrange
        Path referenceFile = createFile("fileA.txt");
        Path poolDir = Files.createDirectory(tempDir.resolve("pool"));
        Path poolFile = createFileIn(poolDir, "file1.txt");

        when(properties.referenceFile()).thenReturn(referenceFile.toString());
        when(properties.poolDirectory()).thenReturn(poolDir.toString());

        when(fileRepository.loadWords(referenceFile)).thenReturn(Set.of("hello", "world"));
        when(fileRepository.loadWords(poolFile)).thenReturn(Set.of("hello", "world"));
        when(fileRepository.listPoolFiles(poolDir)).thenReturn(List.of(poolFile));
        when(similarityCalculator.calculate(any(), any())).thenReturn(100.0);

        // Act
        List<SimilarityResult> results = fileMatcherService.findMatches();

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.getFirst().score()).isEqualTo(100.0);
        assertThat(results.getFirst().fileName()).isEqualTo("file1.txt");
    }

    // Validation — reference file

    @Test
    @DisplayName("Should throw InvalidConfigurationException when reference file does not exist")
    void shouldThrow_whenReferenceFileDoesNotExist() {
        when(properties.referenceFile()).thenReturn(tempDir.resolve("nonexistent.txt").toString());

        assertThatThrownBy(() -> fileMatcherService.findMatches()).isInstanceOf(InvalidConfigurationException.class).hasMessageContaining("Reference file does not exist");
    }

    @Test
    @DisplayName("Should throw InvalidConfigurationException when reference file path is a directory")
    void shouldThrow_whenReferenceFileIsADirectory() throws IOException {
        Path directory = Files.createDirectory(tempDir.resolve("notAFile"));

        when(properties.referenceFile()).thenReturn(directory.toString());

        assertThatThrownBy(() -> fileMatcherService.findMatches()).isInstanceOf(InvalidConfigurationException.class).hasMessageContaining("Reference file path is not a regular file");
    }

    // Validation — pool directory

    @Test
    @DisplayName("Should throw InvalidConfigurationException when pool directory does not exist")
    void shouldThrow_whenPoolDirectoryDoesNotExist() throws IOException {
        Path referenceFile = createFile("fileA.txt");

        when(properties.referenceFile()).thenReturn(referenceFile.toString());
        when(properties.poolDirectory()).thenReturn(tempDir.resolve("nonexistent-dir").toString());

        assertThatThrownBy(() -> fileMatcherService.findMatches()).isInstanceOf(InvalidConfigurationException.class).hasMessageContaining("Pool directory does not exist");
    }

    @Test
    @DisplayName("Should throw InvalidConfigurationException when pool directory path is a file")
    void shouldThrow_whenPoolDirectoryIsAFile() throws IOException {
        Path referenceFile = createFile("fileA.txt");
        Path notADirectory = createFile("notADirectory.txt");

        when(properties.referenceFile()).thenReturn(referenceFile.toString());
        when(properties.poolDirectory()).thenReturn(notADirectory.toString());

        assertThatThrownBy(() -> fileMatcherService.findMatches()).isInstanceOf(InvalidConfigurationException.class).hasMessageContaining("Pool directory path is not a directory");
    }

    // Helpers

    /**
     * Creates an empty regular file directly inside {@code tempDir}.
     */
    private Path createFile(String fileName) throws IOException {
        return Files.createFile(tempDir.resolve(fileName));
    }

    /**
     * Creates an empty regular file inside the given parent directory.
     */
    private Path createFileIn(Path parent, String fileName) throws IOException {
        return Files.createFile(parent.resolve(fileName));
    }
}