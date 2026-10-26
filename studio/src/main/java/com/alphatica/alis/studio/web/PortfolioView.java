package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.studio.logic.riskfreerate.RiskFreeRatesProvider;
import com.alphatica.alis.studio.logic.stats.Stat;
import com.alphatica.alis.studio.logic.trading.account.AccountProvider;
import com.alphatica.alis.studio.logic.trading.portfolio.PortfolioStats;
import com.alphatica.alis.studio.logic.trading.portfolio.PortfolioTables;
import com.alphatica.alis.studio.state.AppState;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.AccountReplayer;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;

import java.io.IOException;
import java.util.List;

/**
 * @param summary tiles under the equity chart: the account now
 * @param stats   table next to the chart: risk, ratios, trades and history
 * @param equity  empty without market data: positions cannot be valued
 */
public record PortfolioView(List<Stat> summary, List<Stat> stats, Table actions, Table history, Equity equity) {

	/**
	 * Replays the account actions. The history has one record per round trip (first buy to last sell), and for
	 * each open position its closed part (if anything was sold) followed by its open part.
	 */
	public static PortfolioView load() throws IOException, AccountActionException {
		Account account = new Account(0);
		List<AccountAction> actions = AccountProvider.getAccountActions();
		MarketData marketData = AppState.getMarketData();
		boolean haveMarketData = marketData != null;
		// Without market data the replay values no sessions: the equity is empty
		AccountReplayer.replayInto(account, marketData, actions);
		Equity equity = Equity.of(account.getAccountHistory().getSessions());
		PortfolioStats stats = PortfolioStats.of(account, marketData, RiskFreeRatesProvider.get());
		return new PortfolioView(stats.summary(), stats.details(),
				new Table(PortfolioTables.ACTIONS_COLUMNS, PortfolioTables.actionRows(actions), "No account actions yet."),
				new Table(PortfolioTables.HISTORY_COLUMNS, PortfolioTables.historyRows(account.getAllHistoryRecords(),
						haveMarketData), "No trades yet."),
				equity);
	}
}
