package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.logic.stats.Stat;
import com.alphatica.alis.studio.logic.trading.account.AccountProvider;
import com.alphatica.alis.studio.logic.trading.backtest.BacktestStats;
import com.alphatica.alis.studio.logic.trading.portfolio.PortfolioTables;
import com.alphatica.alis.studio.state.AppState;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.AccountReplayer;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @param equity empty without market data: positions cannot be valued
 */
public record PortfolioView(List<Stat> stats, Table actions, Table history, Equity equity) {

	/**
	 * Replays the account actions. The history has one record per round trip (first buy to last sell), and for
	 * each open position its closed part (if anything was sold) followed by its open part.
	 */
	public static PortfolioView load() throws IOException, AccountActionException {
		Account account = new Account(0);
		List<AccountAction> actions = AccountProvider.getAccountActions();
		MarketData marketData = AppState.getMarketData();
		boolean haveMarketData = marketData != null;
		List<Time> times = new ArrayList<>();
		List<Double> nav = new ArrayList<>();
		List<Double> netDeposits = new ArrayList<>();
		AccountReplayer.replayInto(account, marketData, actions, (time, deposits) -> {
			if (haveMarketData) {
				times.add(time);
				nav.add(account.getNAV());
				netDeposits.add(deposits);
			}
		});
		return new PortfolioView(BacktestStats.ofPortfolio(account, haveMarketData),
				new Table(PortfolioTables.ACTIONS_COLUMNS, PortfolioTables.actionRows(actions), "No account actions yet."),
				new Table(PortfolioTables.HISTORY_COLUMNS, PortfolioTables.historyRows(account.getAllHistoryRecords(),
						haveMarketData), "No trades yet."),
				Equity.timeWeighted(times, nav, netDeposits));
	}
}
