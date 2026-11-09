package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.studio.logic.table.Column;
import com.alphatica.alis.studio.logic.trading.backtest.BacktestSettings;
import com.alphatica.alis.studio.logic.trading.optimize.MaxPermutations;
import com.alphatica.alis.studio.logic.trading.optimize.OptimizationRequest;
import com.alphatica.alis.studio.strategy.StrategyList;
import com.alphatica.alis.studio.web.jobs.Job;
import com.alphatica.alis.studio.web.jobs.JobRegistry;
import com.alphatica.alis.tools.java.TopResults;
import com.alphatica.alis.trading.optimizer.OptimizerException;
import com.alphatica.alis.trading.optimizer.ParametersSelection;
import com.alphatica.alis.trading.optimizer.StrategyOptimizer;
import com.alphatica.alis.trading.strategy.Strategy;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.alphatica.alis.studio.logic.options.StudioOptions.PARAMETERS_SELECTIONS;
import static com.alphatica.alis.studio.logic.options.StudioOptions.RESULT_VERIFIERS;
import static com.alphatica.alis.studio.logic.options.StudioOptions.SCORERS;
import static com.alphatica.alis.trading.optimizer.ParametersSelection.FULL_PERMUTATION;
import static com.alphatica.alis.trading.optimizer.ParametersSelection.GENETIC;

/**
 * Strategies → Optimization: searches strategy parameters and streams the best 100 results found so far and the
 * number of iterations done.
 */
final class OptimizationApi {
	private static final String PATH = "/api/optimization";
	private static final String JOB_TYPE = "optimization";
	private static final int TOP_RESULTS = 100;

	private static final List<Column> RESULTS = List.of(Column.score("Score"), Column.number("NAV", 0),
			Column.percent("Max DD", 0), Column.number("Return / max downside DD", 2), Column.number("Trades", 0),
			Column.percent("Accuracy", 1),
			Column.number("Expectancy", 2), Column.number("Profit factor", 2), Column.number("Profit per trade", 2),
			Column.percent("Profitable markets", 1));

	private final JobRegistry jobs;
	private volatile TopResults<OptimizationResult> lastResults;

	OptimizationApi(JobRegistry jobs) {
		this.jobs = jobs;
	}

	void register(RoutesConfig routes) {
		routes.get(PATH + "/options", ctx -> ctx.json(options()));
		routes.post(PATH, this::start);
		routes.get(PATH + "/results.csv", this::exportResults);
	}

	private static OptimizationOptions options() {
		Map<String, Map<String, Long>> maxPermutations = new LinkedHashMap<>();
		StrategyList.STRATEGIES.forEach((name, factory) -> {
			Strategy strategy = factory.get();
			Map<String, Long> bySelection = new LinkedHashMap<>();
			PARAMETERS_SELECTIONS.forEach((text, selection) -> bySelection.put(text, MaxPermutations.defaultFor(strategy, selection)));
			maxPermutations.put(name, bySelection);
		});
		return new OptimizationOptions(List.copyOf(StrategyList.STRATEGIES.keySet()), List.copyOf(RESULT_VERIFIERS.keySet()),
				List.copyOf(PARAMETERS_SELECTIONS.keySet()), List.copyOf(SCORERS.keySet()), FULL_PERMUTATION.getText(),
				maxPermutations, RunDefaults.current(), RESULTS);
	}

	private void start(Context ctx) {
		StartRequest request = ctx.bodyAsClass(StartRequest.class);
		MarketData marketData = Inputs.marketData();
		BacktestSettings settings = Inputs.settings(request);
		ParametersSelection selection = Inputs.option(PARAMETERS_SELECTIONS, request.parametersSelection(), "parameters selection");
		long maxPermutations = selection == FULL_PERMUTATION
				? MaxPermutations.defaultFor(settings.strategyFactory().get(), selection)
				: Inputs.wholeNumber(request.maxPermutations(), "max permutations", 1, Long.MAX_VALUE);
		OptimizationRequest optimization = new OptimizationRequest(settings, marketData,
				Inputs.scorer(SCORERS, request.scorer(), marketData),
				Inputs.option(RESULT_VERIFIERS, request.resultVerifier(), "result verifier"), selection, maxPermutations);
		StrategyOptimizer optimizer;
		try {
			optimizer = optimization.createOptimizer();
		} catch (OptimizerException e) {
			throw new ValidationException(e.getMessage());
		}
		Job job = jobs.start(JOB_TYPE, request, running -> run(running, optimizer, selection));
		ctx.json(Map.of("jobId", job.id()));
	}

	private void exportResults(Context ctx) {
		TopResults<OptimizationResult> results = lastResults;
		if (results == null) {
			throw new ValidationException("Run an optimization first");
		}
		ctx.contentType("text/csv; charset=utf-8");
		ctx.header("Content-Disposition", "attachment; filename=\"optimization-results.csv\"");
		ctx.result(csv(results.list()));
	}

	static String csv(List<OptimizationResult> results) {
		return OptimizationCsv.of(RESULTS, results);
	}

	private void run(Job job, StrategyOptimizer optimizer, ParametersSelection selection)
			throws OptimizerException {
		TopResults<OptimizationResult> topResults = topResults(selection);
		lastResults = topResults;
		job.onCancel(optimizer::stop);
		optimizer.optimize((score, account) -> {
			if (topResults.add(OptimizationResult.of(score))) {
				job.channel().replace("results", topResults.list());
			}
			job.channel().replace("iterations", optimizer.getLoopCount());
		});
		job.channel().replace("iterations", optimizer.getLoopCount());
	}

	static TopResults<OptimizationResult> topResults(ParametersSelection selection) {
		Comparator<OptimizationResult> order = Comparator.comparingDouble(r -> -r.score());
		return new TopResults<>(TOP_RESULTS, order, selection == GENETIC ? OptimizationResult::cells : null);
	}

	/**
	 * @param fullPermutation the parameters selection that tries every permutation: max permutations is fixed
	 * @param maxPermutations default max permutations by strategy, then by parameters selection
	 */
	record OptimizationOptions(List<String> strategies, List<String> resultVerifiers, List<String> parametersSelections,
	                           List<String> scorers, String fullPermutation, Map<String, Map<String, Long>> maxPermutations,
	                           RunDefaults defaults, List<Column> results) {
	}

	record StartRequest(String strategy, String startTime, String endTime, String commissionRate, String initialCapital,
	                    String resultVerifier, String parametersSelection, String scorer,
	                    String maxPermutations) implements Inputs.RunForm {
	}
}
