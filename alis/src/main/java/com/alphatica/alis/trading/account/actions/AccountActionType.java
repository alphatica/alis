package com.alphatica.alis.trading.account.actions;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.Account;

public sealed interface AccountActionType permits Trade, Deposit, Withdrawal {
	void doOnAccount(Time time, Account account) throws AccountActionException;

	String toCsv();

}
