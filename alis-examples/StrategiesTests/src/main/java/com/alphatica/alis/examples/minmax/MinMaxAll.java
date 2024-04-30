package com.alphatica.alis.examples.minmax;

import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.MarketScore;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.strategy.StrategyExecutor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.market.MarketFilters.STOCKS;

public class MinMaxAll {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		MarketData stooqData = StooqLoader.load(WORK_DIR);
		List<Market> markets = stooqData.listMarkets(STOCKS);
		for (int length = 120; length <= 250; length += 10) {
			checkLength(length, markets, stooqData);
		}
	}

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	private static void checkLength(int length, List<Market> markets, MarketData stooqData) {
		List<MarketScore> scores = new ArrayList<>();
		MinMaxStrategy minMaxStrategy = new MinMaxStrategy(length);
		for (Market market : markets) {
			MarketData marketData = stooqData.fromSingle(market);
			StrategyExecutor executor = new StrategyExecutor().withInitialCash(100_000)
															  .withTimeRange(new Time(2007_07_09), new Time(2025_01_01));
			Account account = executor.execute(marketData, minMaxStrategy);
			scores.add(new MarketScore(market.getName(), account.getNAV()));
		}
		OptionalDouble average = scores.stream().mapToDouble(MarketScore::value).average();
		if (average.isPresent()) {
			System.out.printf("Length: %d Average profit: %.0f%n", length, average.getAsDouble());
		}
	}
}
