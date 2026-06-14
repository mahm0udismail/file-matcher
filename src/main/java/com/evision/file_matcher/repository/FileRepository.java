package com.evision.file_matcher.repository;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

/**
 * Contract for all file system access operations.
 * The service layer depends on this abstraction — never on the implementation directly.
 */
public interface FileRepository {

    /**
     * Reads a file and returns the set of unique lowercase alphabetic words it contains.
     *
     * @param filePath path to the file to tokenize
     * @return set of unique lowercase alphabetic words
     */
    Set<String> loadWords(Path filePath);

    /**
     * Lists all regular files directly inside the given directory.
     * Does not recurse into subdirectories.
     *
     * @param directoryPath path to the pool directory
     * @return list of paths to regular files found in the directory
     */
    List<Path> listPoolFiles(Path directoryPath);
}