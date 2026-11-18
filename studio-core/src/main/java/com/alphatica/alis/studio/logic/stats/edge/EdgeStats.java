package com.alphatica.alis.studio.logic.stats.edge;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.riskfreerate.RiskFreeRates;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.logic.riskfreerate.RiskFreeRatesProvider;
import com.alphatica.alis.studio.logic.stats.RatioStats;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.BenchmarkReturns;
import com.alphatica.alis.trading.account.ReturnRatios;
import com.alphatica.alis.trading.account.ReturnRatios.BenchmarkSessions;
import com.alphatica.alis.trading.account.SessionHistory;
import com.alphatica.alis.trading.account.attribution.AlphaAttribution;
import com.alphatica.alis.trading.account.attribution.HeldStocks;
import com.alphatica.alis.trading.account.attribution.SharpeAttribution;
import com.alphatica.alis.trading.account.attribution.TreynorAttribution;

import java.util.List;

public final class EdgeStats {

	private EdgeStats() {
	}

	public static boolean hasBenchmark(MarketData marketData) {
		return marketData.getMarket(RatioStats.BENCHMARK) != null;
	}

	/** With the rates of {@link RiskFreeRatesProvider}; arguments as in {@link #of(Account, MarketData, RiskFreeRates)}. */
	public static EdgeView of(Account account, MarketData marketData) {
		return of(account, marketData, RiskFreeRatesProvider.get());
	}

	/**
	 * @param marketData with the benchmark of {@link RatioStats}
	 * @param rates      risk-free rates, of {@link RatioStats#COUNTRY}
	 */
	public static EdgeView of(Account account, MarketData marketData, RiskFreeRates rates) {
		SessionHistory history = account.getAccountHistory().getSessions();
		List<Time> sessions = history.times();
		if (sessions.isEmpty()) {
			return new EdgeView(List.of());
		}
		Market benchmark = marketData.getMarket(RatioStats.BENCHMARK);
		List<Integer> times = sessions.stream().map(Time::time).toList();
		BenchmarkSessions benchmarkSessions = ReturnRatios.benchmarkSessions(history.returns(),
				BenchmarkReturns.at(benchmark, sessions), sessions,
				rates.periods(RatioStats.COUNTRY, sessions.getFirst(), sessions.getLast()));
		double beta = benchmarkSessions.ratios().beta();
		HeldStocks heldStocks = HeldStocks.of(account, marketData, benchmark);
		return new EdgeView(List.of(
				EdgeCards.alpha(AlphaAttribution.of(benchmarkSessions, history.exposureAtStart()), beta),
				EdgeCards.sharpeAndTreynor(SharpeAttribution.of(benchmarkSessions), TreynorAttribution.of(benchmarkSessions),
						beta),
				EdgeCards.heldStocks(times, heldStocks),
				EdgeCards.sizeEffect(times, heldStocks)));
	}
}
