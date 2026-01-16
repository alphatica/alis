package com.alphatica.alis.examples.buyleader;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketDataSet;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;

public class BuyBest {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	public static void main(String[] args) throws ExecutionException, InterruptedException {


//		MarketData stooqData = StooqLoader.loadPL(WORK_DIR);
		MarketData stooqData = StooqLoader.loadUS(WORK_DIR);
		for(int len = 2; len < 500; len++) {
			HashMap<MarketName, Position> positions = new HashMap<>();
			double spent = 0.0;
			double portion = 100.0;
			List<Time> times = stooqData.getTimes().stream().filter(t -> t.isAfter(new Time(2016_01_01))).toList();
			for(Time time: times) {
				var data = TimeMarketDataSet.getCached(time, stooqData);
				var numberOfStocks = data.listUpToDateMarkets(STOCKS).size();
				if (numberOfStocks > 100) {
					MarketName bestName = null;
					var bestScore = Double.NEGATIVE_INFINITY;
					var bestPrice = 0.0;

					for(var market : data.listUpToDateMarkets(STOCKS)) {
						var opens = market.getLayer(Layer.OPEN);
						if (opens.size() < len) {
							continue;
						}
						var pos = positions.computeIfAbsent(market.getMarketName(), marketName -> new Position());
						pos.lastPrice = opens.get(0);
						var score = opens.get(0) / opens.get(len - 1);
						if (score > bestScore) {
							bestScore = score;
							bestName = market.getMarketName();
							bestPrice = opens.get(0);
						}
					}
					var p = positions.computeIfAbsent(bestName, marketName -> new Position());
					if (bestPrice > 0.0) {
						p.count += portion / bestPrice;
						spent += portion;
					}
				}
			}
			var total = positions.values().stream().mapToDouble(p -> p.count * p.lastPrice).sum();
			System.out.println("len: " + len + " " + (total / spent));
		}

	}


}