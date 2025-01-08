package com.alphatica.alis.examples.williamsr;

import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.tools.java.TaskExecutor;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.strategy.StrategyExecutor;

import java.io.File;
import java.util.concurrent.ExecutionException;


public class WilliamsRStrategyTest {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";
	private static final boolean RUN_OPTIMIZATION = false;
	private static final Time OPTIMIZATION_START_TIME = new Time(2007_07_09);
	private static final Time OPTIMIZATION_END_TIME = new Time(2018_01_23);
	private static final Time FINAL_TIME = new Time(2025_01_01);

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		MarketData stooqData = StooqLoader.loadPL(WORK_DIR);

		TaskExecutor<Double> executor = new TaskExecutor<>();
		if (RUN_OPTIMIZATION) {
			executeOptimization(executor, stooqData);
		} else {
			executeOnce(executor, stooqData);
		}
		executor.getResults();
	}

	private static void executeOnce(TaskExecutor<Double> executor, MarketData stooqData) {
		executor.submit(() -> checkParams(124, 0.41, stooqData, OPTIMIZATION_END_TIME, FINAL_TIME));
	}

	private static void executeOptimization(TaskExecutor<Double> executor, MarketData stooqData) {
		for (int length = 5; length <= 360; length += 1) {
			for (double level = 0.01; level < 1.0; level += 0.01) {
				int len = length;
				double lvl = level;
				executor.submit(() -> checkParams(len, lvl, stooqData, OPTIMIZATION_START_TIME, OPTIMIZATION_END_TIME));
			}
		}
	}

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	private static Double checkParams(int length, double level, MarketData marketData, Time startTime, Time endTime) throws AccountActionException {
		WilliamsRStrategy williamsRStrategy = new WilliamsRStrategy(length, level, 1);
		StrategyExecutor executor = new StrategyExecutor().withInitialCash(67529.0).withTimeRange(startTime, endTime);
		Account account = executor.execute(marketData, williamsRStrategy);
		System.out.printf("%.0f %d %d%n", account.getNAV(), length, (int) Math.round(level * 100));
		return account.getNAV();
	}

}

