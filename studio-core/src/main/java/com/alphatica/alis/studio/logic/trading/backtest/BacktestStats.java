package com.alphatica.alis.studio.logic.trading.backtest;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.riskfreerate.RiskFreeRates;
import com.alphatica.alis.studio.logic.riskfreerate.RiskFreeRatesProvider;
import com.alphatica.alis.studio.logic.stats.RatioStats;
import com.alphatica.alis.studio.logic.stats.Stat;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.SessionHistory;
import com.alphatica.alis.trading.account.TradeStats;
import com.alphatica.alis.trading.account.scorer.ReturnOnInvestedCapital;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class BacktestStats {

	private final List<Stat> stats = new ArrayList<>();

	private BacktestStats() {
	}

	/** With the rates of {@link RiskFreeRatesProvider}; arguments as in {@link #of(BacktestResult, MarketData, RiskFreeRates)}. */
	public static List<Stat> of(BacktestResult result, MarketData marketData) {
		return of(result, marketData, RiskFreeRatesProvider.get());
	}

	/**
	 * @param marketData market data of the run, with the benchmark of {@link RatioStats}
	 */
	public static List<Stat> of(BacktestResult result, MarketData marketData, RiskFreeRates rates) {
		BacktestStats backtestStats = new BacktestStats();
		Account account = result.account();
		SessionHistory sessions = account.getAccountHistory().getSessions();
		backtestStats.stats.add(RatioStats.totalReturn(sessions));
		backtestStats.stats.add(RatioStats.cagr(sessions));
		backtestStats.stats.add(Stat.percent("Return on invested capital",
				new ReturnOnInvestedCapital().score(account, Map.of()), 2));
		backtestStats.stats.add(Stat.percent("Max drawdown", account.getMaxDD(), 0));
		backtestStats.stats.add(Stat.percent("Max downside drawdown", account.getMaxDownsideDD(), 0));
		backtestStats.stats.addAll(RatioStats.of(sessions, account.getMaxDD(), account.getMaxDownsideDD(), marketData,
				rates));
		backtestStats.addTradeStats(account.getStats(), true);
		backtestStats.addMarketStats(account);
		return List.copyOf(backtestStats.stats);
	}

	/**
	 * Trade and history stats of the user's portfolio, with the paid commissions; values that need market data are
	 * empty when {@code haveMarketData} is false.
	 */
	public static List<Stat> tradesOf(Account account, boolean haveMarketData) {
		BacktestStats backtestStats = new BacktestStats();
		backtestStats.addTradeStats(account.getStats(), haveMarketData);
		backtestStats.stats.add(Stat.number("Paid commissions", account.getAccountHistory().getPaidCommissions(), 0));
		backtestStats.addMarketStats(account);
		return List.copyOf(backtestStats.stats);
	}

	private void addTradeStats(TradeStats tradeStats, boolean haveMarketData) {
		stats.add(Stat.number("Total trades", tradeStats.trades(), 0));
		stats.add(Stat.number("Profit factor", tradeStats.profitFactor(), 2));
		stats.add(Stat.number("Expectancy", tradeStats.expectancy(), 2));
		stats.add(Stat.percent("Average win", tradeStats.averageWinPercent(), 0));
		stats.add(Stat.percent("Average loss", tradeStats.averageLossPercent(), 0));
		stats.add(Stat.percent("Average profit per trade", tradeStats.profitPerTrade(), 0));
		addIf(haveMarketData, Stat.number("Average trade length", tradeStats.averageTradeLength(), 1));
		stats.add(Stat.percent("Accuracy", tradeStats.accuracy(), 1));
	}

	private void addMarketStats(Account account) {
		stats.add(Stat.number("Profitable markets", account.countProfitableMarkets(), 0));
		stats.add(Stat.number("Unprofitable markets", account.countUnprofitableMarkets(), 0));
		stats.add(Stat.text("Biggest win", account.biggestWin()));
		stats.add(Stat.text("Biggest loss", account.biggestLoss()));
	}

	/** Values that need market data are shown as unknown without it. */
	private void addIf(boolean known, Stat stat) {
		stats.add(known ? stat : Stat.unknown(stat.name()));
	}
}
