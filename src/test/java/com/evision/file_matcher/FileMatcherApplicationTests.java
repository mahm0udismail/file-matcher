package com.evision.file_matcher;

import com.evision.file_matcher.model.SimilarityResult;
import com.evision.file_matcher.service.FileMatcherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("FileMatcherApplication — Integration Test")
@SpringBootTest
class FileMatcherApplicationTests {

	@TempDir
	static Path tempDir;

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) throws IOException {
		// Reference file — File A
		Path referenceFile = tempDir.resolve("fileA.txt");
		Files.writeString(referenceFile,
				"the quick brown fox jumps over the lazy dog"
		);

		// Pool directory
		Path poolDir = Files.createDirectory(tempDir.resolve("pool"));

		// Perfect match — same words, different order
		Files.writeString(poolDir.resolve("perfect_match.txt"),
				"dog lazy the over jumps fox brown quick"
		);

		// Partial match — 4 shared words, 4 new words
		// shared: {the, quick, brown, fox}
		// new:    {hello, world, java, spring}
		// intersection=4, union=4+4+4=12 → 4/12 * 100 = 33.33%
		Files.writeString(poolDir.resolve("partial_match.txt"),
				"the quick brown fox hello world java spring"
		);

		// No match — zero shared words
		Files.writeString(poolDir.resolve("no_match.txt"),
				"apple banana cherry mango grape lemon orange peach"
		);

		registry.add("filematcher.reference-file", referenceFile::toString);
		registry.add("filematcher.pool-directory", poolDir::toString);
	}

	@Autowired
	private FileMatcherService fileMatcherService;

	// Context

	@Test
	@DisplayName("Should load Spring context successfully")
	void shouldLoadSpringContext() {
		assertThat(fileMatcherService).isNotNull();
	}

	// End-to-end scoring

	@Test
	@DisplayName("Should return three results for three pool files")
	void shouldReturnThreeResults() {
		List<SimilarityResult> results = fileMatcherService.findMatches();

		assertThat(results).hasSize(3);
	}

	@Test
	@DisplayName("Should score perfect match as 100%")
	void shouldScorePerfectMatchAs100() {
		List<SimilarityResult> results = fileMatcherService.findMatches();

		SimilarityResult perfectMatch = findByFileName(results, "perfect_match.txt");

		assertThat(perfectMatch.score()).isCloseTo(100.0, within(0.001));
	}

	@Test
	@DisplayName("Should score no match as 0%")
	void shouldScoreNoMatchAs0() {
		List<SimilarityResult> results = fileMatcherService.findMatches();

		SimilarityResult noMatch = findByFileName(results, "no_match.txt");

		assertThat(noMatch.score()).isCloseTo(0.0, within(0.001));
	}

	@Test
	@DisplayName("Should score partial match correctly")
	void shouldScorePartialMatchCorrectly() {
		List<SimilarityResult> results = fileMatcherService.findMatches();

		SimilarityResult partialMatch = findByFileName(results, "partial_match.txt");

		// intersection=4, union=12 → 4/12 * 100 = 33.33%
		assertThat(partialMatch.score()).isCloseTo(33.33, within(0.01));
	}

	@Test
	@DisplayName("Should return results sorted by score descending")
	void shouldReturnResultsSortedByScoreDescending() {
		List<SimilarityResult> results = fileMatcherService.findMatches();

		assertThat(results.get(0).score())
				.isGreaterThanOrEqualTo(results.get(1).score());

		assertThat(results.get(1).score())
				.isGreaterThanOrEqualTo(results.get(2).score());
	}

	@Test
	@DisplayName("Should identify perfect_match.txt as the best match")
	void shouldIdentifyBestMatch() {
		List<SimilarityResult> results = fileMatcherService.findMatches();

		assertThat(results.getFirst().fileName()).isEqualTo("perfect_match.txt");
	}

	// Helper

	/**
	 * Finds a result by file name from the results list.
	 * Fails the test immediately if the file name is not found.
	 */
	private SimilarityResult findByFileName(List<SimilarityResult> results, String fileName) {
		return results.stream()
				.filter(r -> r.fileName().equals(fileName))
				.findFirst()
				.orElseThrow(() -> new AssertionError(
						"Expected result for file '" + fileName + "' not found in results: " + results
				));
	}
}