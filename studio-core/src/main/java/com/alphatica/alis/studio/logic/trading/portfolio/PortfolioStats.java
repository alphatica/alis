package com.alphatica.alis.studio.logic.trading.portfolio;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.riskfreerate.RiskFreeRates;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.logic.stats.RatioStats;
import com.alphatica.alis.studio.logic.stats.Stat;
import com.alphatica.alis.studio.logic.trading.backtest.BacktestStats;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.MarketBeta;
import com.alphatica.alis.trading.account.SessionHistory;

import java.util.ArrayList;
import java.util.List;

/**
 * Stats of the user's portfolio.
 *
 * @param summary the account now: shown as tiles under the equity chart
 * @param details risk, ratios, trades and history: shown in a table next to the chart
 */
public record PortfolioStats(List<Stat> summary, List<Stat> details) {
	/** Beta of a position too new for its own: as the market. */
	private static final double NEW_MARKET_BETA = 1;

	/**
	 * Values that need market data are unknown without it ({@code marketData == null}); benchmark ratios also
	 * without the benchmark in it.
	 *
	 * @param account the replayed account, with the sessions of its history valued at the market data
	 */
	public static PortfolioStats of(Account account, MarketData marketData, RiskFreeRates rates) {
		boolean haveMarketData = marketData != null;
		SessionHistory sessions = account.getAccountHistory().getSessions();
		return new PortfolioStats(summary(account, marketData, sessions),
				details(account, marketData, sessions, rates));
	}

	/**
	 * YTD: time-weighted return since the end of the previous year. Invested: positions at the latest prices.
	 * Exposure: invested as a share of the account value. Beta: the positions' betas against
	 * {@link RatioStats#BENCHMARK} ({@link MarketBeta}, over the year up to the last session, or the sessions there are)
	 * weighted by their value, with beta 0 for the cash and {@link #NEW_MARKET_BETA} for a position without a beta
	 * (under {@link MarketBeta#MIN_SESSIONS} sessions); unknown without the benchmark.
	 */
	private static List<Stat> summary(Account account, MarketData marketData, SessionHistory sessions) {
		boolean haveMarketData = marketData != null;
		double nav = account.getNAV();
		double invested = nav - account.getCash();
		List<Stat> stats = new ArrayList<>();
		stats.add(known(haveMarketData, Stat.number("NAV", nav, 0)));
		stats.add(known(haveMarketData, Stat.percent("YTD", sessions.yearToDateReturnPercent(), 2)));
		stats.add(known(haveMarketData, Stat.number("Invested", invested, 0)));
		stats.add(Stat.number("Cash", account.getCash(), 0));
		stats.add(known(haveMarketData && nav > 0, Stat.percent("Exposure", invested / nav * 100, 0)));
		stats.add(known(haveMarketData && nav > 0, Stat.number("Beta", beta(account, marketData, sessions, nav), 2)));
		stats.add(known(haveMarketData, Stat.percent("Drawdown", account.getCurrentDD(), 0)));
		stats.add(known(haveMarketData, Stat.percent("Downside drawdown", account.getCurrentDownsideDD(), 0)));
		return List.copyOf(stats);
	}

	/** NaN without the benchmark in the market data. */
	private static double beta(Account account, MarketData marketData, SessionHistory sessions, double nav) {
		if (account.getPositions().isEmpty()) {
			return 0;
		}
		Market benchmark = marketData == null ? null : marketData.getMarket(RatioStats.BENCHMARK);
		List<Time> times = sessions.times();
		if (benchmark == null || times.isEmpty()) {
			return Double.NaN;
		}
		double weighted = 0;
		for (MarketName name : account.getPositions().keySet()) {
			Market market = marketData.getMarket(name);
			double beta = market == null ? Double.NaN : MarketBeta.of(market, benchmark, times.getLast());
			weighted += account.getPositionValue(name) * (Double.isNaN(beta) ? NEW_MARKET_BETA : beta);
		}
		return weighted / nav;
	}

	/**
	 * Total profit: account value minus deposits plus withdrawals. Ratios are from the time-weighted session returns,
	 * so deposits and withdrawals do not change them.
	 */
	private static List<Stat> details(Account account, MarketData marketData, SessionHistory sessions,
									  RiskFreeRates rates) {
		boolean haveMarketData = marketData != null;
		List<Stat> stats = new ArrayList<>();
		stats.add(known(haveMarketData, Stat.number("Total profit", account.calcCashProfit(), 0)));
		stats.add(known(haveMarketData, RatioStats.totalReturn(sessions)));
		stats.add(known(haveMarketData, RatioStats.cagr(sessions)));
		stats.add(known(haveMarketData, Stat.percent("Max drawdown", account.getMaxDD(), 0)));
		stats.add(known(haveMarketData, Stat.percent("Max downside drawdown", account.getMaxDownsideDD(), 0)));
		stats.addAll(RatioStats.of(sessions, account.getMaxDD(), account.getMaxDownsideDD(), marketData, rates));
		stats.addAll(BacktestStats.tradesOf(account, haveMarketData));
		return List.copyOf(stats);
	}

	/** Unknown when not known (e.g. it needs market data) or not a finite number (e.g. a ratio without enough data). */
	private static Stat known(boolean known, Stat stat) {
		return known ? Stat.finite(stat) : Stat.unknown(stat.name());
	}
}
