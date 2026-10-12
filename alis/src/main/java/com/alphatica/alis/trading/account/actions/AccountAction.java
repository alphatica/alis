package com.alphatica.alis.trading.account.actions;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.Account;

import java.util.List;

import static java.lang.String.format;

public record AccountAction(Time time, AccountActionType actionType) {

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

	public String toCsv() {
		return format("%d,%s", time.time(), actionType.toCsv());
	}

}
