package com.alphatica.alis.studio.logic.trading.portfolio;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.riskfreerate.RiskFreeRates;
import com.alphatica.alis.studio.logic.stats.RatioStats;
import com.alphatica.alis.studio.logic.stats.Stat;
import com.alphatica.alis.studio.logic.trading.backtest.BacktestStats;
import com.alphatica.alis.trading.account.Account;
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

	/**
	 * Values that need market data are unknown without it ({@code marketData == null}); benchmark ratios also
	 * without the benchmark in it.
	 *
	 * @param account the replayed account, with the sessions of its history valued at the market data
	 */
	public static PortfolioStats of(Account account, MarketData marketData, RiskFreeRates rates) {
		boolean haveMarketData = marketData != null;
		SessionHistory sessions = account.getAccountHistory().getSessions();
		return new PortfolioStats(summary(account, haveMarketData, sessions),
				details(account, marketData, sessions, rates));
	}

	/**
	 * Total return: the latest time-weighted cumulative return, as on the equity chart. Exposure: positions at the
	 * latest prices as a share of the account value.
	 */
	private static List<Stat> summary(Account account, boolean haveMarketData, SessionHistory sessions) {
		double nav = account.getNAV();
		List<Stat> stats = new ArrayList<>();
		stats.add(known(haveMarketData, Stat.number("NAV", nav, 0)));
		stats.add(known(haveMarketData, RatioStats.totalReturn(sessions)));
		stats.add(Stat.number("Cash", account.getCash(), 0));
		stats.add(known(haveMarketData && nav > 0, Stat.percent("Exposure", (nav - account.getCash()) / nav * 100, 0)));
		stats.add(known(haveMarketData, Stat.percent("Drawdown", account.getCurrentDD(), 0)));
		stats.add(known(haveMarketData, Stat.percent("Downside drawdown", account.getCurrentDownsideDD(), 0)));
		return List.copyOf(stats);
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
