package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.logic.trading.backtest.BacktestStats;
import com.alphatica.alis.studio.logic.trading.walkforward.WalkForward;
import com.alphatica.alis.studio.logic.trading.walkforward.WalkForward.OptimizedWindow;
import com.alphatica.alis.studio.logic.trading.walkforward.WalkForward.Result;
import com.alphatica.alis.studio.logic.trading.walkforward.WalkForward.TestedWindow;
import com.alphatica.alis.studio.logic.trading.walkforward.WalkForwardWindows.Window;
import com.alphatica.alis.studio.web.BacktestApi.BacktestView;
import com.alphatica.alis.studio.web.jobs.Job;

import java.util.ArrayList;
import java.util.List;

final class WalkForwardJob {
	private WalkForwardJob() {
	}

	static Result run(Job job, WalkForward walkForward, List<Window> windows, MarketData marketData)
			throws Exception {
		job.onCancel(walkForward::stop);
		Time from = windows.getFirst().testStart();
		Time to = windows.getLast().testEnd();
		int bars = (int) marketData.getTimes().stream().filter(time -> !time.isBefore(from) && !time.isAfter(to)).count();
		BacktestLiveOutput output = new BacktestLiveOutput(job.channel(), bars);
		List<WindowRow> rows = new ArrayList<>();
		Result result = walkForward.run(new WalkForward.Listener() {
			@Override
			public void windowStarted(Window window, int count) {
				job.channel().replace("window", new WindowProgress(window.number(), count));
			}

			@Override
			public void iterations(int iterations) {
				job.channel().replace("iterations", iterations);
			}

			@Override
			public void windowOptimized(OptimizedWindow window) {
				rows.add(WindowRow.of(window, null));
				job.channel().replace("windows", List.copyOf(rows));
			}
		}, output);
		if (result.backtest() == null) {
			return result;
		}
		// Positions closed at the end of the backtest
		output.sendNewTrades(result.backtest().account());
		job.channel().replace("windows", result.windows().stream().map(tested -> WindowRow.of(tested.optimized(), tested))
				.toList());
		job.channel().replace("result", new BacktestView(BacktestStats.of(result.backtest(), marketData),
				Equity.of(result.backtest().account().getAccountHistory().getSessions())));
		return result;
	}

	/**
	 * @param number window being optimized, from 1
	 */
	record WindowProgress(int number, int windows) {
	}

	/**
	 * One row of the windows table; days as yyyymmdd.
	 *
	 * @param returnPercent return of the backtest in the test window; null until the backtest ends
	 * @param transactions  buys and sells in the test window; null until the backtest ends
	 */
	record WindowRow(int number, int trainingStart, int trainingEnd, int testStart, int testEnd, List<String> params,
					 double trainingScore, Double returnPercent, Integer transactions) {

		static WindowRow of(OptimizedWindow optimized, TestedWindow tested) {
			Window window = optimized.window();
			return new WindowRow(window.number(), window.trainingStart().time(), window.trainingEnd().time(),
					window.testStart().time(), window.testEnd().time(), optimized.paramsCode(), optimized.trainingScore(),
					tested == null ? null : tested.returnPercent(), tested == null ? null : tested.transactions());
		}
	}
}
