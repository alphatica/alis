package com.alphatica.alis.examples.buyleader;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketDataSet;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;

public class BuyAll {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	public static void main(String[] args) throws ExecutionException, InterruptedException {
//		MarketData stooqData = StooqLoader.loadPL(WORK_DIR); // Score: 2.23
		MarketData stooqData = StooqLoader.loadUS(WORK_DIR); // Score: 4.93
		BuyLeaderPortfolio portfolio = simulate(stooqData);
		System.out.println("Spent: " + portfolio.spent());
		System.out.println("Total: " + portfolio.totalValue());
	}

	private static BuyLeaderPortfolio simulate(MarketData stooqData) {
		BuyLeaderPortfolio portfolio = new BuyLeaderPortfolio();
		List<Time> times = stooqData.getTimes().stream().filter(t -> t.isAfter(new Time(2016_01_01))).toList();
		for (Time time : times) {
			processSession(stooqData.snapshotAt(time), portfolio);
		}
		return portfolio;
	}

	private static void processSession(TimeMarketDataSet data, BuyLeaderPortfolio portfolio) {
		var markets = data.listUpToDateMarkets(STOCKS);
		if (markets.size() <= 100) {
			return;
		}
		double perStock = 100.0 / markets.size();
		for (var market : markets) {
			double price = market.getData(Layer.OPEN, 0);
			double newSize = perStock / price;
			if (Double.isFinite(newSize) && Double.isFinite(price)) {
				portfolio.buy(market.getMarketName(), perStock, price);
			}
		}
	}
}
