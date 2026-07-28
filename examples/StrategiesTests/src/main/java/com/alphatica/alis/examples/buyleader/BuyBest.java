package com.alphatica.alis.examples.buyleader;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;

import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;

public class BuyBest {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	public static void main(String[] args) throws ExecutionException, InterruptedException {
//		MarketData stooqData = StooqLoader.loadPL(WORK_DIR);
		MarketData stooqData = StooqLoader.loadUS(WORK_DIR);
		List<Time> times = stooqData.getTimes().stream().filter(t -> t.isAfter(new Time(2016_01_01))).toList();
		for (int len = 2; len < 500; len++) {
			BuyLeaderPortfolio portfolio = evaluateLength(stooqData, times, len);
			System.out.println("len: " + len + " " + (portfolio.totalValue() / portfolio.spent()));
		}
	}

	private static BuyLeaderPortfolio evaluateLength(MarketData stooqData, List<Time> times, int len) {
		BuyLeaderPortfolio portfolio = new BuyLeaderPortfolio();
		for (Time time : times) {
			processSession(stooqData.cachedSnapshotAt(time), portfolio, len);
		}
		return portfolio;
	}

	private static void processSession(TimeMarketDataSet data, BuyLeaderPortfolio portfolio, int len) {
		List<TimeMarketData> markets = data.listUpToDateMarkets(STOCKS);
		if (markets.size() <= 100) {
			return;
		}
		findBestCandidate(markets, portfolio, len)
				.filter(candidate -> candidate.price() > 0.0)
				.ifPresent(candidate -> portfolio.buy(candidate.market(), 100.0, candidate.price()));
	}

	private static Optional<Candidate> findBestCandidate(List<TimeMarketData> markets,
												BuyLeaderPortfolio portfolio, int len) {
		Candidate best = null;
		for (TimeMarketData market : markets) {
			var opens = market.getLayer(Layer.OPEN);
			if (opens.size() < len) {
				continue;
			}
			double price = opens.get(0);
			portfolio.updatePrice(market.getMarketName(), price);
			double score = price / opens.get(len - 1);
			if (score > Double.NEGATIVE_INFINITY && (best == null || score > best.score())) {
				best = new Candidate(market.getMarketName(), price, score);
			}
		}
		return Optional.ofNullable(best);
	}

	private record Candidate(MarketName market, double price, double score) {
	}
}
