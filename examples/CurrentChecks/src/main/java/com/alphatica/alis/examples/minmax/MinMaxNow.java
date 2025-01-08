package com.alphatica.alis.examples.minmax;

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

public class MinMaxNow {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		StooqLoader.unzipNewPL(WORK_DIR, "Downloads");
		MarketData stooqData = StooqLoader.loadPL(WORK_DIR);
		Time last = stooqData.getTimes().getLast();
		System.out.println("Last time: " + last);
		MinMax minMax = new MinMax(170);
		final int[] stats = {0, 0};
		for (Market market : stooqData.listMarkets(STOCKS)) {
			TimeMarketData marketData = market.getAt(last);
			if (marketData != null) {
				double minMaxNow = minMax.calculate(marketData);
				if (!Double.isNaN(minMaxNow)) {
					if (minMaxNow > 0.0) {
						stats[0]++;
					} else {
						stats[1]++;
					}
					System.out.printf("Market %s MinMax: %.2f%n", market.getName(), minMaxNow);
				}
			}
		}
		System.out.println("Up: " + stats[0] + " Down: " + stats[1] + " Proportion: " + (double) stats[0] / (stats[0] + stats[1]));
	}
}
