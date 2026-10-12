package com.alphatica.alis.studio.logic.trading.backtest;

import com.alphatica.alis.studio.logic.stats.Stat;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.TradeStats;

import java.util.ArrayList;
import java.util.List;

public final class BacktestStats {

	private final List<Stat> stats = new ArrayList<>();

	private BacktestStats() {
	}

	public static List<Stat> of(BacktestResult result) {
		return of(result.account(), result.missedTrades(), true);
	}

	/**
	 * The same stats for the user's portfolio: without missed trades (there are no orders to miss); values that
	 * need market data are empty when {@code haveMarketData} is false.
	 */
	public static List<Stat> ofPortfolio(Account account, boolean haveMarketData) {
		return of(account, null, haveMarketData);
	}

	private static List<Stat> of(Account account, Integer missedTrades, boolean haveMarketData) {
		BacktestStats backtestStats = new BacktestStats();
		backtestStats.addAccountStats(account, missedTrades == null, haveMarketData);
		backtestStats.addTradeStats(account.getStats(), missedTrades, haveMarketData);
		backtestStats.addHistoryStats(account);
		return List.copyOf(backtestStats.stats);
	}

	private void addAccountStats(Account account, boolean portfolio, boolean haveMarketData) {
		addIf(haveMarketData, Stat.number("Final account value", account.getNAV(), 0));
		if (portfolio) {
			addPortfolioStats(account, haveMarketData);
		}
		addIf(haveMarketData, Stat.percent("Current drawdown", account.getCurrentDD(), 0));
		addIf(haveMarketData, Stat.percent("Max drawdown", account.getMaxDD(), 0));
		addIf(haveMarketData, Stat.percent("Max downside drawdown", account.getMaxDownsideDD(), 0));
	}

	/**
	 * Exposure: positions at the latest prices as a share of the account value. Total profit: account value minus
	 * deposits plus withdrawals.
	 */
	private void addPortfolioStats(Account account, boolean haveMarketData) {
		double nav = account.getNAV();
		stats.add(Stat.number("Free cash", account.getCash(), 0));
		addIf(haveMarketData && nav > 0, Stat.percent("Exposure", (nav - account.getCash()) / nav * 100, 0));
		addIf(haveMarketData, Stat.number("Total profit", account.calcCashProfit(), 0));
	}

	private void addTradeStats(TradeStats tradeStats, Integer missedTrades, boolean haveMarketData) {
		stats.add(Stat.number("Total trades", tradeStats.trades(), 0));
		if (missedTrades != null) {
			stats.add(Stat.number("Missed trades", missedTrades, 0));
		}
		stats.add(Stat.number("Profit factor", tradeStats.profitFactor(), 2));
		stats.add(Stat.number("Expectancy", tradeStats.expectancy(), 2));
		stats.add(Stat.percent("Average win", tradeStats.averageWinPercent(), 0));
		stats.add(Stat.percent("Average loss", tradeStats.averageLossPercent(), 0));
		stats.add(Stat.percent("Average profit per trade", tradeStats.profitPerTrade(), 0));
		addIf(haveMarketData, Stat.number("Average trade length", tradeStats.averageTradeLength(), 1));
		stats.add(Stat.percent("Accuracy", tradeStats.accuracy(), 1));
	}

	private void addHistoryStats(Account account) {
		stats.add(Stat.number("Paid commissions", account.getAccountHistory().getPaidCommissions(), 0));
		stats.add(Stat.number("Profitable markets", account.countProfitableMarkets(), 0));
		stats.add(Stat.number("Unprofitable markets", account.countUnprofitableMarkets(), 0));
		stats.add(Stat.text("Biggest win", account.biggestWin()));
		stats.add(Stat.text("Biggest loss", account.biggestLoss()));
	}

	/** Values that need market data are shown as unknown without it. */
	private void addIf(boolean known, Stat stat) {
		stats.add(known ? stat : new Stat(stat.name(), null, 0, ""));
	}
}
