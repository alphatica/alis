package com.alphatica.alis.trading.account.actions;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.tools.java.MarketAttributes;
import com.alphatica.alis.trading.account.Account;

public interface AccountActionType extends MarketAttributes {
	void doOnAccount(Time time, Account account) throws AccountActionException;

	String toCsv();

}
