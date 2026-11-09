package com.alphatica.alis.studio.web;

import com.alphatica.alis.tools.java.TopResults;
import com.alphatica.alis.trading.optimizer.OptimizerScore;
import com.alphatica.alis.trading.optimizer.ParametersSelection;
import com.alphatica.alis.trading.optimizer.ResultStats;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.alphatica.alis.trading.optimizer.ParametersSelection.FULL_PERMUTATION;
import static com.alphatica.alis.trading.optimizer.ParametersSelection.GENETIC;
import static com.alphatica.alis.trading.optimizer.ParametersSelection.RANDOM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class OptimizationApiTest {

	@Test
	void shouldShowScoreAndStatsInResultColumns() {
		OptimizationResult result = OptimizationResult.of(new OptimizerScore(2.5, Map.of("period", 7),
				new ResultStats(995, -3, -0.25, 4, 75, 0.025, 1.1, 1.25, 60)));

		assertEquals(2.5, result.score());
		assertEquals(Arrays.asList(2.5, 995.0, -3.0, -0.25, 4, 75.0, 0.025, 1.1, 1.25, 60.0), result.cells());
		assertEquals(List.of("period = 7; "), result.params());
	}

	@Test
	void shouldLeaveReturnToDownsideDrawdownEmptyWhenNotFinite() {
		OptimizationResult result = OptimizationResult.of(new OptimizerScore(1, Map.of(),
				new ResultStats(100_000, -0.0, Double.NaN, 0, 0, 0, 0, 0, Double.NaN)));

		assertNull(result.cells().get(3));
	}

	@Test
	void shouldShowBestResultsFirst() {
		TopResults<OptimizationResult> results = OptimizationApi.topResults(RANDOM);

		results.add(result(4, 1, 1000));
		results.add(result(5, 2, 1000));

		assertEquals(List.of(List.of("value = 2; "), List.of("value = 1; ")), params(results));
	}

	@Test
	void shouldShowResultOfSameValuesOnceWithGeneticSelection() {
		TopResults<OptimizationResult> results = OptimizationApi.topResults(GENETIC);

		results.add(result(5, 1, 1000));
		results.add(result(5, 2, 1000));
		results.add(result(5, 3, Math.nextUp(1000.0)));

		assertEquals(List.of(List.of("value = 1; "), List.of("value = 3; ")), params(results));
	}

	@Test
	void shouldShowEveryResultOfSameValuesWithOtherSelections() {
		for (ParametersSelection selection : List.of(FULL_PERMUTATION, RANDOM)) {
			TopResults<OptimizationResult> results = OptimizationApi.topResults(selection);

			results.add(result(5, 1, 1000));
			results.add(result(5, 2, 1000));

			assertEquals(List.of(List.of("value = 1; "), List.of("value = 2; ")), params(results), selection.name());
		}
	}

	@Test
	void shouldExportResultsWithParametersAsCsvColumns() {
		Map<String, Object> best = new LinkedHashMap<>();
		best.put("period", 1000);
		best.put("stop", 0.5 + 0.02 * 9);
		best.put("enabled", true);
		Map<String, Object> second = new LinkedHashMap<>();
		second.put("enabled", false);
		second.put("period", 45);
		second.put("stop", 0.5);
		List<OptimizationResult> results = List.of(
				OptimizationResult.of(new OptimizerScore(2.5, best,
						new ResultStats(12_345_678.9, -3, -0.25, 4, 75, 0.025, 1.1, 1.25, 60))),
				OptimizationResult.of(new OptimizerScore(-1.5, second,
						new ResultStats(990, -0.5, Double.NaN, 0, Double.NaN, 0, Double.POSITIVE_INFINITY, 0.1, 0))));

		assertEquals("""
				Score,NAV,Max DD,Return / max downside DD,Trades,Accuracy,Expectancy,Profit factor,Profit per trade,Profitable markets,period,stop,enabled
				2.5,12345678.9,-3.0,-0.25,4,75.0,0.025,1.1,1.25,60.0,1000,0.68,true
				-1.5,990.0,-0.5,,0,NaN,0.0,Infinity,0.1,0.0,45,0.5,false
				""", OptimizationApi.csv(results));
	}

	@Test
	void shouldExportOnlyHeaderWithoutResults() {
		assertEquals("Score,NAV,Max DD,Return / max downside DD,Trades,Accuracy,Expectancy,Profit factor,"
				+ "Profit per trade,Profitable markets\n", OptimizationApi.csv(List.of()));
	}

	@Test
	void shouldNotSendParameterValuesToGui() {
		String json = Json.write(result(5, 7, 1000));

		assertFalse(json.contains("paramValues"), json);
		assertEquals(List.of("value = 7; "), result(5, 7, 1000).params());
	}

	private static OptimizationResult result(double score, int value, double nav) {
		return OptimizationResult.of(new OptimizerScore(score, Map.of("value", value),
				new ResultStats(nav, -3, 0.5, 4, 75, 0.025, 1.1, 1.25, 60)));
	}

	private static List<List<String>> params(TopResults<OptimizationResult> results) {
		return results.list().stream().map(OptimizationResult::params).toList();
	}
}
