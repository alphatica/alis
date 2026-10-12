package com.alphatica.alis.studio.logic.trading.backtest;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.strategy.BarExecutedConsumer;
import com.alphatica.alis.trading.strategy.StrategyExecutor;

import java.util.ArrayList;
import java.util.List;

public final class BacktestRunner {

	private BacktestRunner() {
	}

	/**
	 * @param barListener called after every session, e.g. to show the orders and trades while the backtest runs
	 */
	public static BacktestResult run(BacktestSettings settings, MarketData marketData, BarExecutedConsumer barListener)
			throws AccountActionException {
		List<Time> times = new ArrayList<>();
		List<Double> nav = new ArrayList<>();
		StrategyExecutor executor = new StrategyExecutor().withTimeRange(settings.timeStart(), settings.timeEnd())
														  .withCommissionRate(settings.commissionRate())
														  .withInitialCash(settings.initialCapital())
														  .withBarExecutedConsumer((time, account, orders) -> {
															  times.add(time);
															  nav.add(account.getNAV());
															  barListener.execute(time, account, orders);
														  });
		Account account = executor.execute(marketData, settings.strategyFactory().get());
		// The executor closes all positions after the last session, which changes its value
		if (!nav.isEmpty()) {
			nav.set(nav.size() - 1, account.getNAV());
		}
		return new BacktestResult(account, executor.getMissedTrades(), times, nav);
	}
}
