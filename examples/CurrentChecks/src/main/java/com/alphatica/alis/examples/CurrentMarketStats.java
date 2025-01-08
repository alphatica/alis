package com.alphatica.alis.examples;

import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.indicators.Indicator;
import com.alphatica.alis.indicators.trend.MinMax;
import com.alphatica.alis.tools.math.Regressions;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.market.MarketFilters.STOCKS;

public class CurrentMarketStats {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		StooqLoader.unzipNewPL(WORK_DIR, "Downloads");
		MarketData stooqData = StooqLoader.loadPL(WORK_DIR);
		showMarketBreadth(stooqData);
	}

	private static void showMarketBreadth(MarketData stooqData) {
		divider("Market breadth");
		Time last = stooqData.getTimes().getLast();
		Indicator indicator = getQuadraticRegressionSlopeIndicator(430);
		double currentLevel = getMarketBreadth(stooqData, last, indicator);
		System.out.println("Current level (required above 0.65 for bull market): " + currentLevel);
	}

	private static double getMarketBreadth(MarketData data, Time time, Indicator indicator) {
		int count = 0;
		int aboveZero = 0;
		for (Market market : data.listMarkets(STOCKS)) {
			TimeMarketData marketData = market.getAt(time);
			if (marketData == null) {
				continue;
			}
			double value = indicator.calculate(marketData);
			if (Double.isNaN(value)) {
				continue;
			}
			if (value > 0) {
				aboveZero++;
			}
			count++;
		}
		if (count > 100) {
			return (double) aboveZero / count;
		} else {
			return Double.NaN;
		}
	}

	private static Indicator getQuadraticRegressionSlopeIndicator(int len) {
		return new Indicator() {
			@Override
			public double calculate(TimeMarketData timeMarketData) {
				double v0 = Regressions.calculateQuadraticRegression(timeMarketData, len, 0, 0);
				double v1 = Regressions.calculateQuadraticRegression(timeMarketData, len, 0, 1);
				return v0 - v1;
			}
		};
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
