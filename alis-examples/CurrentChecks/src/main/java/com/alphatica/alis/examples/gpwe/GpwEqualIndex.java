package com.alphatica.alis.examples.gpwe;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.market.MarketFilters.STOCKS;

public class GpwEqualIndex {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		StooqLoader.unzipNew(WORK_DIR, "Downloads");
		MarketData stooqData = StooqLoader.load(WORK_DIR);
		Time start = new Time(2018_01_23);
		Time end = new Time(2025_01_01);
		int count = 0;
		double changes = 0;
		for (Market market : stooqData.listMarkets(STOCKS)) {
			TimeMarketData first = market.getAtOrNext(start);
			TimeMarketData last = market.getAtOrPrevious(end);
			if (first != null && last != null && last.getTime().isAfter(first.getTime())) {
				count++;
				double change = last.getData(Layer.CLOSE, 0) / first.getData(Layer.CLOSE, 0);
				changes += change;
			}
		}
		if (count > 0) {
			double changePercent = ((changes / count) - 1.0) * 100;
			System.out.printf("Average change: %.1f%% x%.2f%n", changePercent, changes / count);
		}
	}
}
