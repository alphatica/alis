package com.alphatica.alis.examples;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.market.MarketFilters.STOCKS;

public class BiggestMVIndex {

	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		MarketData stooqData = StooqLoader.load(WORK_DIR);
		List<Time> times = stooqData.getTimes().stream().filter(t -> t.isAfter(new Time(2004_01_01L))).toList();
		for(Time time : times) {
			showValueAt(time, stooqData);
		}

	}

	private static void showValueAt(Time time, MarketData stooqData) {
		List<Double> mvs = new ArrayList<>();
		for(Market market: stooqData.listMarkets(STOCKS)) {
			TimeMarketData marketData = market.getAt(time);
			if (marketData != null) {
				double mv = marketData.getData(Layer.MV, 0);
				if (!Double.isNaN(mv) && mv > 0) {
					mvs.add(mv);
				}
			}
		}
		if (mvs.size() < 100) {
			return;
		}
		Collections.sort(mvs);
		Collections.reverse(mvs);
		List<Double> limited = mvs.subList(0, 100);
		System.out.println(time + " " + limited.stream().mapToDouble(v -> v).sum());
	}

}
