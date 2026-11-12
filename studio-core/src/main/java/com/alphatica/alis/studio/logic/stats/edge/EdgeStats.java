package com.alphatica.alis.studio.logic.stats.edge;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.logic.stats.RatioStats;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.BenchmarkReturns;
import com.alphatica.alis.trading.account.SessionHistory;
import com.alphatica.alis.trading.account.attribution.HeldStocks;

import java.util.List;

public final class EdgeStats {

	private EdgeStats() {
	}

	public static boolean hasBenchmark(MarketData marketData) {
		return marketData.getMarket(RatioStats.BENCHMARK) != null;
	}

	public static EdgeView of(Account account, MarketData marketData) {
		SessionHistory history = account.getAccountHistory().getSessions();
		List<Time> sessions = history.times();
		if (sessions.isEmpty()) {
			return new EdgeView(List.of());
		}
		Market benchmark = marketData.getMarket(RatioStats.BENCHMARK);
		return new EdgeView(EdgeTimeline.of(sessions, history.returns(), history.exposure(),
				BenchmarkReturns.atOrZero(benchmark, sessions),
				HeldStocks.of(account, marketData, benchmark, sessions)));
	}
}
