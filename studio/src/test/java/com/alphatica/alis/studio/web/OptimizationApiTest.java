package com.alphatica.alis.studio.web;

import com.alphatica.alis.studio.web.OptimizationApi.Result;
import com.alphatica.alis.tools.java.TopResults;
import com.alphatica.alis.trading.optimizer.OptimizerScore;
import com.alphatica.alis.trading.optimizer.ParametersSelection;
import com.alphatica.alis.trading.optimizer.ResultStats;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.alphatica.alis.trading.optimizer.ParametersSelection.FULL_PERMUTATION;
import static com.alphatica.alis.trading.optimizer.ParametersSelection.GENETIC;
import static com.alphatica.alis.trading.optimizer.ParametersSelection.RANDOM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OptimizationApiTest {

	@Test
	void shouldShowScoreAndStatsInResultColumns() {
		Result result = Result.of(new OptimizerScore(2.5, Map.of("period", 7),
				new ResultStats(995, -3, -0.25, 4, 75, 0.025, 1.1, 1.25, 60)));

		assertEquals(2.5, result.score());
		assertEquals(Arrays.asList(2.5, 995.0, -3.0, -0.25, 4, 75.0, 0.025, 1.1, 1.25, 60.0), result.cells());
		assertEquals(List.of("period = 7; "), result.params());
	}

	@Test
	void shouldLeaveReturnToDownsideDrawdownEmptyWhenNotFinite() {
		Result result = Result.of(new OptimizerScore(1, Map.of(),
				new ResultStats(100_000, -0.0, Double.NaN, 0, 0, 0, 0, 0, Double.NaN)));

		assertNull(result.cells().get(3));
	}

	@Test
	void shouldShowBestResultsFirst() {
		TopResults<Result> results = OptimizationApi.topResults(RANDOM);

		results.add(result(4, 1, 1000));
		results.add(result(5, 2, 1000));

		assertEquals(List.of(List.of("value = 2; "), List.of("value = 1; ")), params(results));
	}

	@Test
	void shouldShowResultOfSameValuesOnceWithGeneticSelection() {
		TopResults<Result> results = OptimizationApi.topResults(GENETIC);

		results.add(result(5, 1, 1000));
		results.add(result(5, 2, 1000));
		results.add(result(5, 3, Math.nextUp(1000.0)));

		assertEquals(List.of(List.of("value = 1; "), List.of("value = 3; ")), params(results));
	}

	@Test
	void shouldShowEveryResultOfSameValuesWithOtherSelections() {
		for (ParametersSelection selection : List.of(FULL_PERMUTATION, RANDOM)) {
			TopResults<Result> results = OptimizationApi.topResults(selection);

			results.add(result(5, 1, 1000));
			results.add(result(5, 2, 1000));

			assertEquals(List.of(List.of("value = 1; "), List.of("value = 2; ")), params(results), selection.name());
		}
	}

	private static Result result(double score, int value, double nav) {
		return Result.of(new OptimizerScore(score, Map.of("value", value),
				new ResultStats(nav, -3, 0.5, 4, 75, 0.025, 1.1, 1.25, 60)));
	}

	private static List<List<String>> params(TopResults<Result> results) {
		return results.list().stream().map(Result::params).toList();
	}
}
