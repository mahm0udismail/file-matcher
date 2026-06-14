package com.evision.file_matcher.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("JaccardSimilarityCalculator")
class JaccardSimilarityCalculatorTest {

    private JaccardSimilarityCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new JaccardSimilarityCalculator();
    }

    // Perfect match

    @Test
    @DisplayName("Should return 100.0 when both sets are identical")
    void shouldReturn100_whenSetsAreIdentical() {
        Set<String> reference = Set.of("the", "quick", "brown", "fox");
        Set<String> candidate = Set.of("the", "quick", "brown", "fox");

        double score = calculator.calculate(reference, candidate);

        assertThat(score).isCloseTo(100.0, within(0.001));
    }

    @Test
    @DisplayName("Should return 100.0 when both sets are empty")
    void shouldReturn100_whenBothSetsAreEmpty() {
        double score = calculator.calculate(Set.of(), Set.of());

        assertThat(score).isCloseTo(100.0, within(0.001));
    }

    @Test
    @DisplayName("Should return 100.0 when both sets contain a single identical word")
    void shouldReturn100_whenSingleWordMatches() {
        Set<String> reference = Set.of("hello");
        Set<String> candidate = Set.of("hello");

        double score = calculator.calculate(reference, candidate);

        assertThat(score).isCloseTo(100.0, within(0.001));
    }

    // No match

    @Test
    @DisplayName("Should return 0.0 when sets are completely disjoint")
    void shouldReturn0_whenSetsAreDisjoint() {
        Set<String> reference = Set.of("apple", "banana", "cherry");
        Set<String> candidate = Set.of("dog", "elephant", "fox");

        double score = calculator.calculate(reference, candidate);

        assertThat(score).isCloseTo(0.0, within(0.001));
    }

    @Test
    @DisplayName("Should return 0.0 when reference is empty and candidate has words")
    void shouldReturn0_whenReferenceIsEmpty() {
        Set<String> reference = Set.of();
        Set<String> candidate = Set.of("dog", "cat");

        double score = calculator.calculate(reference, candidate);

        assertThat(score).isCloseTo(0.0, within(0.001));
    }

    @Test
    @DisplayName("Should return 0.0 when candidate is empty and reference has words")
    void shouldReturn0_whenCandidateIsEmpty() {
        Set<String> reference = Set.of("dog", "cat");
        Set<String> candidate = Set.of();

        double score = calculator.calculate(reference, candidate);

        assertThat(score).isCloseTo(0.0, within(0.001));
    }

    @Test
    @DisplayName("Should return 0.0 when single words do not match")
    void shouldReturn0_whenSingleWordDoesNotMatch() {
        Set<String> reference = Set.of("hello");
        Set<String> candidate = Set.of("world");

        double score = calculator.calculate(reference, candidate);

        assertThat(score).isCloseTo(0.0, within(0.001));
    }

    // Partial match — verified manually

    @Test
    @DisplayName("Should return 50.0 when half the words overlap")
    void shouldReturn50_whenHalfTheWordsOverlap() {
        // reference = {a, b}  candidate = {a, c}
        // intersection = {a} = 1
        // union        = {a, b, c} = 3
        // score        = 1/3 * 100 = 33.33...
        // Actually for 50%: reference={a,b}, candidate={b,c}
        // intersection={b}=1, union={a,b,c}=3 → 33.33
        // For exactly 50%: reference={a,b}, candidate={a,b,c,d}
        // intersection=2, union=4 → 50%
        Set<String> reference = Set.of("a", "b");
        Set<String> candidate = Set.of("a", "b", "c", "d");

        double score = calculator.calculate(reference, candidate);

        // intersection=2, union=4 → 2/4 * 100 = 50.0
        assertThat(score).isCloseTo(50.0, within(0.001));
    }

    @Test
    @DisplayName("Should return 33.33 when one of three words overlap")
    void shouldReturn33_whenOneOfThreeWordsOverlap() {
        // reference = {a, b, c}  candidate = {a, d, e}
        // intersection = {a} = 1
        // union        = {a, b, c, d, e} = 5
        // score        = 1/5 * 100 = 20.0
        // For 33.33: reference={a,b}, candidate={a,c}
        // intersection=1, union=3 → 33.33%
        Set<String> reference = Set.of("a", "b");
        Set<String> candidate = Set.of("a", "c");

        double score = calculator.calculate(reference, candidate);

        // intersection=1, union=3 → 1/3 * 100 = 33.33
        assertThat(score).isCloseTo(33.33, within(0.01));
    }

    @Test
    @DisplayName("Should return correct score when candidate has extra words not in reference")
    void shouldReturnReducedScore_whenCandidateHasExtraWords() {
        // reference = {a, b, c}  candidate = {a, b, c, d, e, f}
        // intersection = 3, union = 6 → 3/6 * 100 = 50.0
        Set<String> reference = Set.of("a", "b", "c");
        Set<String> candidate = Set.of("a", "b", "c", "d", "e", "f");

        double score = calculator.calculate(reference, candidate);

        assertThat(score).isCloseTo(50.0, within(0.001));
    }

    @Test
    @DisplayName("Should return correct score when candidate is missing words from reference")
    void shouldReturnReducedScore_whenCandidateIsMissingWords() {
        // reference = {a, b, c, d}  candidate = {a, b}
        // intersection = 2, union = 4 → 2/4 * 100 = 50.0
        Set<String> reference = Set.of("a", "b", "c", "d");
        Set<String> candidate = Set.of("a", "b");

        double score = calculator.calculate(reference, candidate);

        assertThat(score).isCloseTo(50.0, within(0.001));
    }

    // Symmetry — Jaccard must be symmetric: score(A,B) == score(B,A)

    @Test
    @DisplayName("Should produce the same score regardless of argument order")
    void shouldBeSymmetric() {
        Set<String> setA = Set.of("the", "quick", "brown", "fox");
        Set<String> setB = Set.of("the", "lazy", "brown", "dog");

        double scoreAB = calculator.calculate(setA, setB);
        double scoreBA = calculator.calculate(setB, setA);

        assertThat(scoreAB).isCloseTo(scoreBA, within(0.001));
    }
}