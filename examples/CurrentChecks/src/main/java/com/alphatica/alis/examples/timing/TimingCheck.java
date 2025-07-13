package com.alphatica.alis.examples.timing;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TimingCheck {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		MarketData data = StooqLoader.loadPL(WORK_DIR);
		MarketName market = new MarketName("spx");
		Signal signal = new BuyWithSma(5, false);
//		Signal signal = new AlwaysSignal();
		double result = check(data, signal, market);
		System.out.println(result);
	}

	private static double check(MarketData data, Signal signal, MarketName market) {
		final double ADD_EACH_TIME = 1.0;
		double spent = 0.0;
		double budget = 0.0;
		double position = 0.0;
		int transactions = 0;
		double lastClose = Double.NaN;

		Time startTime = new Time(2000_01_01);
		List<Time> times = data.getTimes().stream().filter(t -> !t.isBefore(startTime)).toList();
		for(Time time: times) {
			TimeMarketData marketNow = data.getMarket(market).getAt(time);
			if (marketNow == null) {
				continue;
			}
			lastClose = marketNow.getData(Layer.CLOSE, 0);
			budget += ADD_EACH_TIME;
			if (signal.shouldBuy(marketNow)) {
				position += budget / lastClose;
				spent += budget;
				budget = 0;
				transactions++;
			}
		}

		System.out.printf("transactions: %d%n", transactions);
		System.out.printf("spent: %.0f%n", spent);
		System.out.printf("size: %.1f%n", position);
		System.out.printf("budget: %.0f%n", budget);
		System.out.printf("Value: %.0f%n", budget + position * lastClose);
		System.out.printf("Profit: %.1f%n", (position * lastClose) / spent);
		return budget + position * lastClose;
	}
}
