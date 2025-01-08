package com.alphatica.alis.trading.account.actions;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.Account;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static java.lang.String.format;

public record Withdrawal(double cash) implements AccountActionType {
	@Override
	public void doOnAccount(Time time, Account account) throws AccountActionException {
		account.reduceCash(cash);
	}

	@Override
	public String toCsv() {
		return format(Locale.US, "Withdrawal,%.2f", cash);
	}

	@Override
	public String toString() {
		return "Withdrawing cash: " + cash;
	}

	@Override
	public Map<String, String> toAttributes() {
		HashMap<String, String> m = new HashMap<>();
		m.put("Type", "Withdraw cash");
		m.put("Value", format(Locale.US, "%.2f", cash));
		return m;
	}
}
