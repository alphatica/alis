package com.alphatica.alis.examples.donchian;

import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.tools.java.GreenLambdaExecutor;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.strategy.StrategyExecutor;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class DonchianOptimization {

	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	public static void main(String[] args) throws ExecutionException, InterruptedException {
		MarketData stooqData = StooqLoader.load(WORK_DIR);
		boolean bestGrowthSort = true;
		int bestBuyPeriod = 0;
		int bestSellPeriod = 0;
		double bestAccountNav = 0;
		GreenLambdaExecutor<DonchianResult> executor = new GreenLambdaExecutor<>();
		submitTasks(executor, stooqData);
		System.out.println("Waiting for results...");
		List<DonchianResult> results = executor.results();
		for (DonchianResult result : results) {
			if (result.finalNav() > bestAccountNav) {
				bestAccountNav = result.finalNav();
				bestSellPeriod = result.sellPeriod();
				bestBuyPeriod = result.buyPeriod();
				bestGrowthSort = result.sortByHighestGrowth();
			}
		}
		System.out.printf("Best: %.0f %d %d %s", bestAccountNav, bestSellPeriod, bestBuyPeriod, bestGrowthSort);
	}

	private static void submitTasks(GreenLambdaExecutor<DonchianResult> executor, MarketData data) {
		int sellPeriod = 5;
		while (sellPeriod <= 375) {
			int buyPeriod = 5;
			while (buyPeriod < 375) {
				int s = sellPeriod;
				int b = buyPeriod;
				executor.submit(() -> checkDonchian(data, s, b, true));
				executor.submit(() -> checkDonchian(data, s, b, false));
				buyPeriod++;
			}
			sellPeriod++;
		}
	}

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	private static DonchianResult checkDonchian(MarketData data, int sellPeriod, int buyPeriod, boolean sortByHighestGrowth) {
		StrategyExecutor executor = new StrategyExecutor().withInitialCash(100_000)
														  .withTimeRange(new Time(2007_07_09), new Time(2018_01_23));
		Account account = executor.execute(data, new DonchianStrategy(buyPeriod, sellPeriod, sortByHighestGrowth, false));
		System.out.printf("%d %d %s %.0f%n", buyPeriod, sellPeriod, sortByHighestGrowth, account.getNAV());
		return new DonchianResult(buyPeriod, sellPeriod, sortByHighestGrowth, account.getNAV());
	}

}
