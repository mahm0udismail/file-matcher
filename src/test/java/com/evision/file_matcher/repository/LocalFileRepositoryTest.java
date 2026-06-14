package com.evision.file_matcher.repository;

import com.evision.file_matcher.exception.FileProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("LocalFileRepository")
class LocalFileRepositoryTest {

    @TempDir
    Path tempDir;

    private LocalFileRepository repository;

    @BeforeEach
    void setUp() {
        repository = new LocalFileRepository();
    }

    // loadWords — happy path

    @Test
    @DisplayName("Should extract only alphabetic words and lowercase them")
    void shouldExtractAlphabeticWordsLowercased() throws IOException {
        Path file = createFile("test.txt", "The Quick Brown Fox");

        Set<String> words = repository.loadWords(file);

        assertThat(words).containsExactlyInAnyOrder("the", "quick", "brown", "fox");
    }

    @Test
    @DisplayName("Should filter out numeric tokens")
    void shouldFilterOutNumericTokens() throws IOException {
        Path file = createFile("test.txt", "hello 123 world 456");

        Set<String> words = repository.loadWords(file);

        assertThat(words).containsExactlyInAnyOrder("hello", "world");
    }

    @Test
    @DisplayName("Should filter out mixed alphanumeric tokens")
    void shouldFilterOutMixedAlphanumericTokens() throws IOException {
        Path file = createFile("test.txt", "hello world123 foo2bar valid");

        Set<String> words = repository.loadWords(file);

        assertThat(words).containsExactlyInAnyOrder("hello", "world", "foo", "bar", "valid");
    }

    @Test
    @DisplayName("Should filter out punctuation-only tokens")
    void shouldFilterOutPunctuationTokens() throws IOException {
        Path file = createFile("test.txt", "hello, world! foo... bar?");

        Set<String> words = repository.loadWords(file);

        assertThat(words).containsExactlyInAnyOrder("hello", "world", "foo", "bar");
    }

    @Test
    @DisplayName("Should deduplicate words across the file")
    void shouldDeduplicateWords() throws IOException {
        Path file = createFile("test.txt", "apple banana apple cherry banana apple");

        Set<String> words = repository.loadWords(file);

        assertThat(words).containsExactlyInAnyOrder("apple", "banana", "cherry");
    }

    @Test
    @DisplayName("Should collect words from all lines of a multi-line file")
    void shouldCollectWordsFromAllLines() throws IOException {
        Path file = createFile("test.txt",
                "the quick brown",
                "fox jumps over",
                "the lazy dog"
        );

        Set<String> words = repository.loadWords(file);

        assertThat(words).containsExactlyInAnyOrder(
                "the", "quick", "brown", "fox", "jumps", "over", "lazy", "dog"
        );
    }

    @Test
    @DisplayName("Should return empty set for an empty file")
    void shouldReturnEmptySet_whenFileIsEmpty() throws IOException {
        Path file = createFile("empty.txt");

        Set<String> words = repository.loadWords(file);

        assertThat(words).isEmpty();
    }

    @Test
    @DisplayName("Should return empty set when file contains only non-alphabetic content")
    void shouldReturnEmptySet_whenFileHasNoAlphabeticContent() throws IOException {
        Path file = createFile("test.txt", "123 456 !!! ??? ###");

        Set<String> words = repository.loadWords(file);

        assertThat(words).isEmpty();
    }

    @Test
    @DisplayName("Should treat words case-insensitively — uppercase and lowercase same word")
    void shouldTreatWordsAsCaseInsensitive() throws IOException {
        Path file = createFile("test.txt", "Hello HELLO hello HeLLo");

        Set<String> words = repository.loadWords(file);

        assertThat(words).containsExactly("hello");
    }

    // loadWords — error handling

    @Test
    @DisplayName("Should throw FileProcessingException when file does not exist")
    void shouldThrowFileProcessingException_whenFileDoesNotExist() {
        Path nonExistent = tempDir.resolve("ghost.txt");

        assertThatThrownBy(() -> repository.loadWords(nonExistent))
                .isInstanceOf(FileProcessingException.class)
                .hasMessageContaining("Failed to read file");
    }

    // listPoolFiles — happy path

    @Test
    @DisplayName("Should list all regular files in a directory")
    void shouldListAllRegularFiles() throws IOException {
        createFile("file1.txt", "hello");
        createFile("file2.txt", "world");
        createFile("file3.txt", "foo");

        List<Path> files = repository.listPoolFiles(tempDir);

        assertThat(files)
                .hasSize(3)
                .allMatch(Files::isRegularFile);
    }

    @Test
    @DisplayName("Should exclude subdirectories from pool listing")
    void shouldExcludeSubdirectories() throws IOException {
        createFile("file1.txt", "hello");
        Files.createDirectory(tempDir.resolve("subdir"));

        List<Path> files = repository.listPoolFiles(tempDir);

        assertThat(files)
                .hasSize(1)
                .allMatch(Files::isRegularFile);
    }

    @Test
    @DisplayName("Should return empty list when directory is empty")
    void shouldReturnEmptyList_whenDirectoryIsEmpty() {
        List<Path> files = repository.listPoolFiles(tempDir);

        assertThat(files).isEmpty();
    }

    // listPoolFiles — error handling

    @Test
    @DisplayName("Should throw FileProcessingException when directory does not exist")
    void shouldThrowFileProcessingException_whenDirectoryDoesNotExist() {
        Path nonExistent = tempDir.resolve("ghost-dir");

        assertThatThrownBy(() -> repository.listPoolFiles(nonExistent))
                .isInstanceOf(FileProcessingException.class)
                .hasMessageContaining("Failed to list files in directory");
    }

    // Helper

    /**
     * Creates a file in the temp directory with the given lines as content.
     * Passing no lines creates an empty file.
     */
    private Path createFile(String fileName, String... lines) throws IOException {
        Path file = tempDir.resolve(fileName);
        Files.write(file, List.of(lines));
        return file;
    }
}