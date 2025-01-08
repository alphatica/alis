package com.alphatica.alis.studio.logic.trading.portfolio;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.studio.state.AppState;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.PositionPricesRecord;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;

import java.util.List;

import static com.alphatica.alis.trading.account.actions.AccountAction.performActionsForTime;

public class PortfolioProvider {
	private PortfolioProvider() {
	}

	public static List<PositionPricesRecord> getPricesRecords(List<AccountAction> accountActions, Account account) throws AccountActionException {
		MarketData marketData = AppState.getMarketData();
		List<Time> times = getTimes(accountActions, marketData);
		for (Time time : times) {
			double change = performActionsForTime(time, accountActions, account);
			if (marketData != null) {
				TimeMarketDataSet timeMarketDataSet = TimeMarketDataSet.build(time, marketData);
				account.updateLastKnown(timeMarketDataSet);
			}
			if (change != 0) {
				System.out.println(time.time() + " " + account.getNAV() + " " + change);
			} else {
				System.out.println(time.time() + " " + account.getNAV());
			}
		}
		return account.getAllPricesRecords();
	}

	private static List<Time> getTimes(List<AccountAction> accountActions, MarketData marketData) {
		if (marketData != null && !accountActions.isEmpty()) {
			return marketData.getTimes().stream().filter(t -> !t.isBefore(accountActions.getFirst().time())).toList();
		} else {
			return accountActions.stream().map(AccountAction::time).toList();
		}
	}
}
