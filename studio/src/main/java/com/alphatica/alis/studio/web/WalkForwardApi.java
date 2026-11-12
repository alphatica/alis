package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.logic.table.Column;
import com.alphatica.alis.studio.logic.trading.backtest.BacktestSettings;
import com.alphatica.alis.studio.logic.trading.optimize.MaxPermutations;
import com.alphatica.alis.studio.logic.trading.walkforward.WalkForward;
import com.alphatica.alis.studio.logic.trading.walkforward.WalkForward.Result;
import com.alphatica.alis.studio.logic.trading.walkforward.WalkForwardWindows;
import com.alphatica.alis.studio.logic.trading.walkforward.WalkForwardWindows.Window;
import com.alphatica.alis.studio.strategy.StrategyList;
import com.alphatica.alis.studio.web.jobs.Job;
import com.alphatica.alis.studio.web.jobs.JobRegistry;
import com.alphatica.alis.trading.optimizer.ParametersSelection;
import com.alphatica.alis.trading.strategy.Strategy;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.alphatica.alis.studio.logic.options.StudioOptions.PARAMETERS_SELECTIONS;
import static com.alphatica.alis.studio.logic.options.StudioOptions.RESULT_VERIFIERS;
import static com.alphatica.alis.studio.logic.options.StudioOptions.SCORERS;
import static com.alphatica.alis.trading.optimizer.ParametersSelection.FULL_PERMUTATION;

/**
 * Research → Walk-forward: optimizes the strategy on every training window, as on the Optimization screen, and trades
 * each test window after it with the best parameters, all test windows in one backtest ({@link WalkForward}).
 * <p>
 * Events: {@code window} (the window being optimized), {@code iterations}, {@code windows} (rows of the windows
 * optimized so far; at the end with their test results), then as in a backtest {@code progress}, {@code orders},
 * {@code trades} and {@code result}. Stop ends the optimization of the current window and backtests the windows
 * optimized before it. The last finished result can be analysed for the sources of its edge.
 */
final class WalkForwardApi {
	static final int DEFAULT_TRAINING_MONTHS = 36;
	static final int DEFAULT_TEST_MONTHS = 12;
	private static final String PATH = "/api/walk-forward";
	private static final String JOB_TYPE = "walk-forward";

	private final JobRegistry jobs;
	private volatile LastRun lastRun;

	WalkForwardApi(JobRegistry jobs) {
		this.jobs = jobs;
	}

	void register(RoutesConfig routes) {
		routes.get(PATH + "/options", ctx -> ctx.json(options()));
		routes.get(PATH + "/windows", this::windows);
		routes.post(PATH, this::start);
		routes.post(PATH + "/edge", ctx -> {
			LastRun run = lastRun;
			if (run == null) {
				throw new ValidationException("Run a walk-forward first");
			}
			ctx.json(EdgeAnalysis.of(run.result().backtest().account(), run.marketData()));
		});
	}

	private static WalkForwardOptions options() {
		Map<String, Map<String, Long>> maxPermutations = new LinkedHashMap<>();
		StrategyList.STRATEGIES.forEach((name, factory) -> {
			Strategy strategy = factory.get();
			Map<String, Long> bySelection = new LinkedHashMap<>();
			PARAMETERS_SELECTIONS.forEach((text, selection) ->
					bySelection.put(text, MaxPermutations.defaultFor(strategy, selection)));
			maxPermutations.put(name, bySelection);
		});
		return new WalkForwardOptions(List.copyOf(StrategyList.STRATEGIES.keySet()), List.copyOf(RESULT_VERIFIERS.keySet()),
				List.copyOf(PARAMETERS_SELECTIONS.keySet()), List.copyOf(SCORERS.keySet()), FULL_PERMUTATION.getText(),
				maxPermutations, RunDefaults.current(), DEFAULT_TRAINING_MONTHS, DEFAULT_TEST_MONTHS, BacktestApi.ORDERS,
				BacktestApi.TRADES);
	}

	/** Preview below the form: the windows of the dates. Never fails: problems are in the answer. */
	private void windows(Context ctx) {
		try {
			MarketData marketData = Inputs.marketData();
			List<Window> windows = windowsOf(marketData, Inputs.time(ctx.queryParam("start"), "start time"),
					Inputs.time(ctx.queryParam("end"), "end time"), ctx.queryParam("trainingMonths"),
					ctx.queryParam("testMonths"));
			ctx.json(new WindowsPreview(windows.size(), windows.getFirst().trainingStart().time(),
					windows.getLast().testEnd().time(), null));
		} catch (ValidationException e) {
			ctx.json(new WindowsPreview(0, null, null, e.getMessage()));
		}
	}

	private void start(Context ctx) {
		StartRequest request = ctx.bodyAsClass(StartRequest.class);
		MarketData marketData = Inputs.marketData();
		BacktestSettings settings = Inputs.settings(request);
		List<Window> windows = windowsOf(marketData, settings.timeStart(), settings.timeEnd(), request.trainingMonths(),
				request.testMonths());
		ParametersSelection selection = Inputs.option(PARAMETERS_SELECTIONS, request.parametersSelection(),
				"parameters selection");
		long maxPermutations = selection == FULL_PERMUTATION
				? MaxPermutations.defaultFor(settings.strategyFactory().get(), selection)
				: Inputs.wholeNumber(request.maxPermutations(), "max permutations", 1, Long.MAX_VALUE);
		WalkForward walkForward = new WalkForward(settings, marketData, Inputs.scorer(SCORERS, request.scorer(), marketData),
				Inputs.option(RESULT_VERIFIERS, request.resultVerifier(), "result verifier"), selection, maxPermutations,
				windows);
		lastRun = null;
		Job job = jobs.start(JOB_TYPE, request, running -> {
			Result result = WalkForwardJob.run(running, walkForward, windows, marketData);
			if (result.backtest() != null) {
				lastRun = new LastRun(result, marketData);
			}
		});
		ctx.json(Map.of("jobId", job.id()));
	}

	/**
	 * Windows of the dates with test sessions in the market data; the first training window needs data from its first
	 * day.
	 */
	static List<Window> windowsOf(MarketData marketData, Time start, Time end, String trainingMonths, String testMonths) {
		int training = (int) Inputs.wholeNumber(trainingMonths, "training window", 1, WalkForwardWindows.MAX_MONTHS);
		int test = (int) Inputs.wholeNumber(testMonths, "test window", 1, WalkForwardWindows.MAX_MONTHS);
		if (start.isAfter(end)) {
			throw new ValidationException("Start time is after end time");
		}
		List<Time> times = marketData.getTimes();
		if (times.isEmpty()) {
			throw new ValidationException("No data loaded. Load market data first");
		}
		Time lastSession = times.getLast();
		List<Window> windows = WalkForwardWindows.of(start, end, training, test).stream()
				.filter(window -> !window.testStart().isAfter(lastSession))
				.toList();
		if (windows.isEmpty()) {
			throw new ValidationException("The data ends on " + lastSession + ", before the first test window");
		}
		Time firstTraining = windows.getFirst().trainingStart();
		if (times.getFirst().isAfter(firstTraining)) {
			throw new ValidationException("The first training window starts on " + firstTraining + ", the data on "
					+ times.getFirst() + ": start later or train on fewer months");
		}
		return windows;
	}

	/**
	 * @param windows            test windows of the dates
	 * @param firstTrainingStart first day of the first training window, yyyymmdd; null with an error
	 * @param lastTestEnd        last day of the last test window, yyyymmdd; null with an error
	 * @param error              why the walk-forward cannot start; null when it can
	 */
	record WindowsPreview(int windows, Integer firstTrainingStart, Integer lastTestEnd, String error) {
	}

	/**
	 * @param fullPermutation the parameters selection that tries every permutation: max permutations is fixed
	 * @param maxPermutations default max permutations by strategy, then by parameters selection
	 * @param orders          columns of the orders of the backtest of the test windows
	 * @param trades          columns of its trades
	 */
	record WalkForwardOptions(List<String> strategies, List<String> resultVerifiers, List<String> parametersSelections,
							  List<String> scorers, String fullPermutation, Map<String, Map<String, Long>> maxPermutations,
							  RunDefaults defaults, int trainingMonths, int testMonths, List<Column> orders,
							  List<Column> trades) {
	}

	private record LastRun(Result result, MarketData marketData) {
	}

	record StartRequest(String strategy, String startTime, String endTime, String commissionRate, String initialCapital,
						String resultVerifier, String parametersSelection, String scorer, String maxPermutations,
						String trainingMonths, String testMonths) implements Inputs.RunForm {
	}
}
