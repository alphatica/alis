package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;

import java.util.ArrayList;
import java.util.List;

import static com.alphatica.alis.trading.account.actions.AccountAction.performActionsForTime;

public final class AccountReplayer {

	private AccountReplayer() {
	}

	public static Account replay(
			MarketData marketData,
			List<AccountAction> originalActions,
			double commissionRate) throws AccountActionException {
		Account account = new Account(0);
		replayInto(account, marketData, originalActions);
		account.close(commissionRate);
		return account;
	}

	/**
	 * Applies actions to the account, updating prices for every market time since the first action.
	 * Without market data ({@code marketData == null}) only the actions' own times are visited.
	 */
	public static void replayInto(
			Account account,
			MarketData marketData,
			List<AccountAction> originalActions) throws AccountActionException {
		replayInto(account, marketData, originalActions, (time, netDeposits) -> {
		});
	}

	/**
	 * Like {@link #replayInto(Account, MarketData, List)}, calling {@code afterTime} once every visited time is done
	 * (actions applied and prices updated), e.g. to record the account's NAV.
	 */
	public static void replayInto(
			Account account,
			MarketData marketData,
			List<AccountAction> originalActions,
			AfterTime afterTime) throws AccountActionException {
		List<AccountAction> actions = new ArrayList<>(originalActions);
		for (Time time : simulationTimes(actions, marketData)) {
			double netDeposits = performActionsForTime(time, actions, account);
			if (marketData != null) {
				account.updateLastKnown(marketData.snapshotAt(time));
			}
			afterTime.accept(time, netDeposits);
		}
	}

	private static List<Time> simulationTimes(List<AccountAction> actions, MarketData marketData) {
		if (actions.isEmpty()) {
			return List.of();
		}
		if (marketData == null) {
			return actions.stream().map(AccountAction::time).toList();
		}
		Time firstActionTime = actions.getFirst().time();
		return marketData.getTimes().stream()
				.filter(time -> !time.isBefore(firstActionTime))
				.toList();
	}

	@FunctionalInterface
	public interface AfterTime {
		/**
		 * @param netDeposits deposits minus withdrawals applied at this time
		 */
		void accept(Time time, double netDeposits);
	}
}
