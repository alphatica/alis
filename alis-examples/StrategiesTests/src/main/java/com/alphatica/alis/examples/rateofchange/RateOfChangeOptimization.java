package com.alphatica.alis.examples.rateofchange;

import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.tools.java.TaskExecutor;
import com.alphatica.alis.tools.math.Statistics;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.strategy.StrategyExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.alphatica.alis.data.market.MarketFilters.STOCKS;

public class RateOfChangeOptimization {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		Random random = new Random();
		MarketData marketData = StooqLoader.load(WORK_DIR);
		TaskExecutor<Double> lambdaExecutor = new TaskExecutor<>();
		for(int task = 0; task < 1000; task++) {
			lambdaExecutor.submit(() -> {

				int bp = random.nextInt(5, 376);
				int sp = random.nextInt(5, 376);
				double bc = random.nextDouble(0, 100);
				double sc = random.nextDouble(-90, 0);
				checkStrategyWithCrossValidation(marketData, bp, sp, bc, sc, new Time(2008_01_23), new Time(2018_01_23));
				return 0.0;
			});
		}

		lambdaExecutor.getResults();
	}

	public static void mainVerify(String[] args) throws ExecutionException, InterruptedException {
		Random random = new Random();
		MarketData marketData = StooqLoader.load(WORK_DIR);
		checkStrategy(marketData, 107, 26, 34, -7, new Time(2018_01_23), new Time(2025_01_01));
		checkStrategy(marketData, 108, 369, 59, -79, new Time(2018_01_23), new Time(2025_01_01));
		checkStrategy(marketData, 97, 369, 84, -82, new Time(2018_01_23), new Time(2025_01_01));
		checkStrategy(marketData, 86, 180, 50, -76, new Time(2018_01_23), new Time(2025_01_01));
		checkStrategy(marketData, 45, 22, 62, -36, new Time(2018_01_23), new Time(2025_01_01));
		checkStrategy(marketData, 145, 67, 85, -61, new Time(2018_01_23), new Time(2025_01_01));
		checkStrategy(marketData, 147, 280, 60, -56, new Time(2018_01_23), new Time(2025_01_01));
		checkStrategy(marketData, 109, 290, 59, -61, new Time(2018_01_23), new Time(2025_01_01));
		checkStrategy(marketData, 50, 28, 64, -50, new Time(2018_01_23), new Time(2025_01_01));
		checkStrategy(marketData, 134, 152, 99, -73, new Time(2018_01_23), new Time(2025_01_01));

	}
		private static double checkStrategyWithCrossValidation(MarketData marketData, int bp, int sp, double bc, double sc, Time startTime, Time endTime) {
		Random r = new Random();
		List<Double> navs = new ArrayList<>();
		for(int attempt = 0; attempt < 50; attempt++) {
			Set<MarketName> allowedMarkets = marketData.listMarkets(m -> m.getType() == MarketType.STOCK && r.nextBoolean())
													   .stream()
													   .map(Market::getName)
													   .collect(Collectors.toSet());
			RateOfChangeStrategy rateOfChangeStrategy = new RateOfChangeStrategy(allowedMarkets, bp, sp, bc, sc);
			StrategyExecutor strategyExecutor = new StrategyExecutor().withTimeRange(startTime, endTime);
			Account account = strategyExecutor.execute(marketData, rateOfChangeStrategy);
			navs.add(account.getNAV());
		}
		Collections.sort(navs);
		double median = Statistics.median(navs);
		System.out.printf("%.2f %d %d %.0f %.0f%n", median, bp, sp, bc, sc);
		return median;
	}

	private static double checkStrategy(MarketData marketData, int bp, int sp, double bc, double sc, Time startTime, Time endTime) {
		Random r = new Random();
		Set<MarketName> allowedMarkets = marketData.listMarkets(STOCKS)
												   .stream()
												   .map(Market::getName)
												   .collect(Collectors.toSet());
		RateOfChangeStrategy rateOfChangeStrategy = new RateOfChangeStrategy(allowedMarkets, bp, sp, bc, sc);
		StrategyExecutor strategyExecutor = new StrategyExecutor().withTimeRange(startTime, endTime);
		Account account = strategyExecutor.execute(marketData, rateOfChangeStrategy);
		System.out.printf("%.2f %d %d %.0f %.0f%n", account.getNAV(), bp, sp, bc, sc);
		return account.getNAV();
	}
}
