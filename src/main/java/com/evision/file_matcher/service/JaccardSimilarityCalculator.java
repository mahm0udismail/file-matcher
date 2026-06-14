package com.evision.file_matcher.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Computes similarity between two word sets using the Jaccard Similarity coefficient.
 * <p>
 * Formula:
 * score = ( |A ∩ B| / |A ∪ B| ) × 100
 * <p>
 * Properties:
 * - Identical sets produce 100.0
 * - Completely disjoint sets produce 0.0
 * - Extra or missing words in either set reduce the score below 100.0
 * - Word order is irrelevant — operates purely on sets
 */
@Slf4j
@Component
public class JaccardSimilarityCalculator implements SimilarityCalculator {

    @Override
    public double calculate(Set<String> referenceWords, Set<String> candidateWords) {

        // Two empty files share the same content — nothing. Score is 100.0.
        if (referenceWords.isEmpty() && candidateWords.isEmpty()) {
            log.debug("Both word sets are empty — returning 100.0");
            return 100.0;
        }

        int intersectionSize = computeIntersectionSize(referenceWords, candidateWords);

        // |A ∪ B| = |A| + |B| - |A ∩ B|  →  avoids allocating a third Set
        int unionSize = referenceWords.size() + candidateWords.size() - intersectionSize;

        double score = ((double) intersectionSize / unionSize) * 100.0;

        log.debug("Jaccard: intersection={}, union={}, score={}", intersectionSize, unionSize, score);

        return score;
    }

    /**
     * Counts shared words by iterating the smaller set and checking membership
     * in the larger set — minimizes the number of hash lookups.
     * <p>
     * Time complexity: O(min(|A|, |B|)) average case with HashSet.
     *
     * @param setA first word set
     * @param setB second word set
     * @return number of words present in both sets
     */
    private int computeIntersectionSize(Set<String> setA, Set<String> setB) {
        Set<String> smaller = setA.size() <= setB.size() ? setA : setB;
        Set<String> larger = setA.size() <= setB.size() ? setB : setA;

        int count = 0;
        for (String word : smaller) {
            if (larger.contains(word)) {
                count++;
            }
        }
        return count;
    }
}