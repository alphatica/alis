package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.studio.logic.stats.RatioStats;
import com.alphatica.alis.studio.logic.stats.edge.EdgeStats;
import com.alphatica.alis.studio.logic.stats.edge.EdgeView;
import com.alphatica.alis.trading.account.Account;

final class EdgeAnalysis {

	private EdgeAnalysis() {
	}

	static EdgeView of(Account account, MarketData marketData) {
		requireBenchmark(marketData);
		return EdgeStats.of(account, marketData);
	}

	private static void requireBenchmark(MarketData marketData) {
		if (!EdgeStats.hasBenchmark(marketData)) {
			throw new ValidationException("Edge analysis needs the benchmark " + RatioStats.BENCHMARK.name()
					+ " in the market data");
		}
	}
}
