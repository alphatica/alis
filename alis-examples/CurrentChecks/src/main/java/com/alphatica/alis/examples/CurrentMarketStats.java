package com.alphatica.alis.examples;

import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.indicators.trend.MinMax;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.market.MarketFilters.STOCKS;

public class CurrentMarketStats {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		StooqLoader.unzipNew(WORK_DIR, "Downloads");
		MarketData stooqData = StooqLoader.load(WORK_DIR);
		showMinMaxRatio(stooqData);
	}

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	private static void divider(String name) {
		System.out.println("_______________________________________________________________________________________");
		System.out.println(name + ":");
	}

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	private static void showMinMaxRatio(MarketData stooqData) {
		divider("Min-Max Ratio");
		Time last = stooqData.getTimes().getLast();
		final int[] stats = getMinMaxStats(stooqData, last);
		System.out.printf("%.3f (+%d -%d)%n", (double) stats[0] / (stats[0] + stats[1]), stats[0], stats[1]);
	}

	private static int[] getMinMaxStats(MarketData stooqData, Time time) {
		MinMax minMax = new MinMax(260);
		final int[] stats = {0, 0};
		for (Market market : stooqData.listMarkets(STOCKS)) {
			TimeMarketData marketData = market.getAt(time);
			if (marketData != null) {
				double minMaxNow = minMax.calculate(marketData);
				if (!Double.isNaN(minMaxNow)) {
					if (minMaxNow > 0.0) {
						stats[0]++;
					} else {
						stats[1]++;
					}
				}
			}
		}
		return stats;
	}


}
