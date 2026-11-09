package com.alphatica.alis.studio.web;

import com.alphatica.alis.trading.optimizer.OptimizerScore;
import com.alphatica.alis.trading.optimizer.ResultStats;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One row of the optimization results table.
 *
 * @param cells       values of the {@link OptimizationApi#RESULTS} columns
 * @param params      the parameters as Java code, one field per line
 * @param paramValues the parameters by name, for the CSV export
 */
record OptimizationResult(double score, List<Object> cells, List<String> params,
						  @JsonIgnore Map<String, Object> paramValues) {

	static OptimizationResult of(OptimizerScore score) {
		ResultStats stats = score.stats();
		double returnToDownsideDD = stats.returnToMaxDownsideDD();
		return new OptimizationResult(score.score(), Arrays.asList(score.score(), stats.nav(), stats.maxDD(),
				Double.isFinite(returnToDownsideDD) ? returnToDownsideDD : null, stats.trades(), stats.accuracy(),
				stats.expectancy(), stats.profitFactor(), stats.profitPerTrade(),
				stats.profitableMarketsPercent()), score.formatParamsAsJavaCode(), new LinkedHashMap<>(score.params()));
	}
}
