package com.alphatica.alis.studio.logic.trading.backtest;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.strategy.BarExecutedConsumer;
import com.alphatica.alis.trading.strategy.StrategyExecutor;

public final class BacktestRunner {

	private BacktestRunner() {
	}

	/**
	 * @param barListener called after every session, e.g. to show the orders and trades while the backtest runs
	 */
	public static BacktestResult run(BacktestSettings settings, MarketData marketData, BarExecutedConsumer barListener)
			throws AccountActionException {
		StrategyExecutor executor = new StrategyExecutor().withTimeRange(settings.timeStart(), settings.timeEnd())
														  .withCommissionRate(settings.commissionRate())
														  .withInitialCash(settings.initialCapital())
														  .withBarExecutedConsumer(barListener);
		Account account = executor.execute(marketData, settings.strategyFactory().get());
		return new BacktestResult(account, executor.getMissedTrades());
	}
}
