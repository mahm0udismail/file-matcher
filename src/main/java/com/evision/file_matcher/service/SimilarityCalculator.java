package com.evision.file_matcher.service;

import java.util.Set;

public interface SimilarityCalculator {

    /**
     * Calculates a similarity score between two sets of words.
     *
     * @param referenceWords word set extracted from the reference file (File A)
     * @param candidateWords word set extracted from a pool file
     * @return similarity score in the range [0.0, 100.0]
     * where 100.0 means identical word sets and 0.0 means no words in common
     */
    double calculate(Set<String> referenceWords, Set<String> candidateWords);
}