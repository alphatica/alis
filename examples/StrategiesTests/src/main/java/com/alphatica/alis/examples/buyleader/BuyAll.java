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

public class BuyAll {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	public static void main(String[] args) throws ExecutionException, InterruptedException {
		HashMap<MarketName, Position> positions = new HashMap<>();
		double spent = 0.0;
		double portion = 100.0;

//		MarketData stooqData = StooqLoader.loadPL(WORK_DIR); // Score: 2.23
		MarketData stooqData = StooqLoader.loadUS(WORK_DIR); // Score: 4.93
		List<Time> times = stooqData.getTimes().stream().filter(t -> t.isAfter(new Time(2016_01_01))).toList();
		for(Time time: times) {
			var data = TimeMarketDataSet.build(time, stooqData);
			var numberOfStocks = data.listUpToDateMarkets(STOCKS).size();
			if (numberOfStocks > 100) {
				var perStock = portion / numberOfStocks;
				for(var market : data.listUpToDateMarkets(STOCKS)) {
					var p = positions.computeIfAbsent(market.getMarketName(), marketName -> new Position());
					var newSize = perStock / market.getData(Layer.OPEN, 0);
					if (!Double.isFinite(newSize)) {
						continue;
					}
					var lastPrice = market.getData(Layer.OPEN, 0);
					if (!Double.isFinite(newSize) || !Double.isFinite(lastPrice)) {
						continue;
					}
					p.count += newSize;
					p.lastPrice = lastPrice;
					spent += perStock;
				}
			}
		}
		System.out.println("Spent: " + spent);
		var total = positions.values().stream().mapToDouble(p -> p.count * p.lastPrice).sum();
		System.out.println("Total: " + total);
	}


}

class Position {
	double count;
	double lastPrice;
}