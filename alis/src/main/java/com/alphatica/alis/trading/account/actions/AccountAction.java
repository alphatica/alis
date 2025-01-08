package com.alphatica.alis.trading.account.actions;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.tools.java.MarketAttributes;
import com.alphatica.alis.trading.account.Account;

import java.util.List;
import java.util.Map;

import static java.lang.String.format;

public record AccountAction(Time time, AccountActionType actionType) implements MarketAttributes {

	public static double performActionsForTime(Time time, List<AccountAction> accountActions, Account account) throws AccountActionException {
		double change = 0;
		while (!accountActions.isEmpty() && !accountActions.getFirst().time().isAfter(time)) {
			AccountAction accountAction = accountActions.removeFirst();
			if (accountAction.actionType instanceof Deposit(double cash)) {
				change += cash;
			}
			if (accountAction.actionType instanceof Withdrawal(double cash)) {
				change -= cash;
			}
			accountAction.actionType().doOnAccount(time, account);
		}
		return change;
	}

	@Override
	public Map<String, String> toAttributes() {
		Map<String, String> m = actionType.toAttributes();
		m.put("Time", time.toString());
		return m;
	}

	public String toCsv() {
		return format("%d,%s", time.time(), actionType.toCsv());
	}

}
