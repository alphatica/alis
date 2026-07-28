package com.alphatica.alis.studio.view.window.analysis;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;

import java.util.ArrayList;
import java.util.List;

import static com.alphatica.alis.trading.account.actions.AccountAction.performActionsForTime;

final class OriginalAccountReplayer {

	private OriginalAccountReplayer() {
	}

	static Account replay(
			MarketData marketData,
			List<AccountAction> originalActions,
			double commissionRate) throws AccountActionException {
		List<AccountAction> actions = new ArrayList<>(originalActions);
		Account account = new Account(0);
		for (Time time : simulationTimes(actions, marketData)) {
			performActionsForTime(time, actions, account);
			TimeMarketDataSet currentData = marketData.snapshotAt(time);
			account.updateLastKnown(currentData);
		}
		account.close(commissionRate);
		return account;
	}

	private static List<Time> simulationTimes(List<AccountAction> actions, MarketData marketData) {
		if (actions.isEmpty()) {
			return List.of();
		}
		Time firstActionTime = actions.getFirst().time();
		return marketData.getTimes().stream()
				.filter(time -> !time.isBefore(firstActionTime))
				.toList();
	}
}
