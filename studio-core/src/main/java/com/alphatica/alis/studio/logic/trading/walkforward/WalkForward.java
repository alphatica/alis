package com.alphatica.alis.studio.logic.trading.walkforward;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.logic.trading.backtest.BacktestResult;
import com.alphatica.alis.studio.logic.trading.backtest.BacktestRunner;
import com.alphatica.alis.studio.logic.trading.backtest.BacktestSettings;
import com.alphatica.alis.studio.logic.trading.optimize.OptimizationRequest;
import com.alphatica.alis.studio.logic.trading.walkforward.WalkForwardWindows.Window;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.scorer.AccountScorer;
import com.alphatica.alis.trading.optimizer.OptimizerException;
import com.alphatica.alis.trading.optimizer.ParametersSelection;
import com.alphatica.alis.trading.optimizer.ResultVerifier;
import com.alphatica.alis.trading.optimizer.StrategyOptimizer;
import com.alphatica.alis.trading.strategy.BarExecutedConsumer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Walk-forward test of a strategy: every training window is optimized as on the Optimization screen and its best
 * parameters trade the test window after it. The test windows are one continuous backtest: at the start of each the
 * strategy switches to the new parameters and keeps its open positions ({@link WalkForwardStrategy}).
 * <p>
 * The best parameters of a window have the highest score; of equal scores, the first by their Java code. Stop ends the
 * optimization of the current window, which is then left out: the backtest covers the windows optimized in full.
 */
public final class WalkForward {
	private final BacktestSettings settings;
	private final MarketData marketData;
	private final Supplier<AccountScorer> scorerFactory;
	private final ResultVerifier resultVerifier;
	private final ParametersSelection parametersSelection;
	private final long maxPermutations;
	private final List<Window> windows;
	private volatile boolean stopped;
	private volatile StrategyOptimizer current;

	/**
	 * @param settings strategy, commission and capital of every backtest; its times are not used
	 */
	public WalkForward(BacktestSettings settings, MarketData marketData, Supplier<AccountScorer> scorerFactory,
					   ResultVerifier resultVerifier, ParametersSelection parametersSelection, long maxPermutations,
					   List<Window> windows) {
		if (windows.isEmpty()) {
			throw new IllegalArgumentException("No windows");
		}
		this.settings = settings;
		this.marketData = marketData;
		this.scorerFactory = scorerFactory;
		this.resultVerifier = resultVerifier;
		this.parametersSelection = parametersSelection;
		this.maxPermutations = maxPermutations;
		this.windows = List.copyOf(windows);
	}

	/** Follows the walk-forward; calls come from the thread of {@link #run} and from optimizer threads, one at a time. */
	public interface Listener {
		/** The optimization of the window starts. */
		void windowStarted(Window window, int windows);

		/** Backtests of the current window's optimization done so far. */
		void iterations(int iterations);

		/** The window's best parameters are known. */
		void windowOptimized(OptimizedWindow window);
	}

	/**
	 * @param params        best parameters by field name
	 * @param paramsCode    the same as Java code, one field per line
	 * @param trainingScore their score in the training window
	 * @param iterations    backtests of the optimization
	 */
	public record OptimizedWindow(Window window, Map<String, Object> params, List<String> paramsCode, double trainingScore,
								  int iterations) {
	}

	/**
	 * @param returnPercent return of the backtest in the test window, compounded over its sessions
	 * @param transactions  buys and sells in the test window
	 */
	public record TestedWindow(OptimizedWindow optimized, double returnPercent, int transactions) {
	}

	/**
	 * @param windows  the windows optimized in full, in order
	 * @param backtest the backtest of their test windows; null when no window was optimized
	 */
	public record Result(List<TestedWindow> windows, BacktestResult backtest) {
	}

	/** Ends the optimization of the current window and starts no more. */
	public void stop() {
		stopped = true;
		StrategyOptimizer optimizer = current;
		if (optimizer != null) {
			optimizer.stop();
		}
	}

	/**
	 * @param bars called after every session of the backtest of the test windows
	 */
	public Result run(Listener listener, BarExecutedConsumer bars) throws OptimizerException, AccountActionException {
		List<OptimizedWindow> optimized = new ArrayList<>();
		for (Window window : windows) {
			if (stopped) {
				break;
			}
			listener.windowStarted(window, windows.size());
			OptimizedWindow result = optimize(window, seeds(optimized), listener);
			if (result == null) {
				break;
			}
			optimized.add(result);
			listener.windowOptimized(result);
		}
		if (optimized.isEmpty()) {
			return new Result(List.of(), null);
		}
		BacktestResult backtest = backtest(optimized, bars);
		return new Result(TestedWindows.of(optimized, backtest.account()), backtest);
	}

	private List<Map<String, Object>> seeds(List<OptimizedWindow> optimized) {
		if (optimized.isEmpty() || parametersSelection == ParametersSelection.FULL_PERMUTATION) {
			return List.of();
		}
		return List.of(optimized.getLast().params());
	}

	/** The window's best parameters; null when stopped before the optimization ended. */
	private OptimizedWindow optimize(Window window, List<Map<String, Object>> seeds, Listener listener)
			throws OptimizerException {
		BacktestSettings training = new BacktestSettings(window.trainingStart(), window.trainingEnd(),
				settings.commissionRate(), settings.initialCapital(), settings.strategyFactory());
		StrategyOptimizer optimizer = new OptimizationRequest(training, marketData, scorerFactory, resultVerifier,
				parametersSelection, maxPermutations).createOptimizer();
		current = optimizer;
		BestParams best = new BestParams();
		if (!stopped) {
			optimizer.optimize((score, account) -> {
				best.offer(score);
				listener.iterations(optimizer.getLoopCount());
			}, seeds);
			// The count grows after each call above: the last one comes now
			listener.iterations(optimizer.getLoopCount());
		}
		current = null;
		if (stopped) {
			return null;
		}
		if (best.isEmpty()) {
			throw new OptimizerException("No parameters were scored in the training window " + window.trainingStart()
					+ " - " + window.trainingEnd());
		}
		return new OptimizedWindow(window, best.params(), best.code(), best.score(), optimizer.getLoopCount());
	}

	private BacktestResult backtest(List<OptimizedWindow> optimized, BarExecutedConsumer bars)
			throws AccountActionException {
		Map<Time, Map<String, Object>> schedule = new LinkedHashMap<>();
		for (OptimizedWindow window : optimized) {
			schedule.put(window.window().testStart(), window.params());
		}
		WalkForwardStrategy strategy = new WalkForwardStrategy(settings.strategyFactory().get(), schedule);
		BacktestSettings test = new BacktestSettings(optimized.getFirst().window().testStart(),
				optimized.getLast().window().testEnd(), settings.commissionRate(), settings.initialCapital(),
				() -> strategy);
		return BacktestRunner.run(test, marketData, bars);
	}
}
