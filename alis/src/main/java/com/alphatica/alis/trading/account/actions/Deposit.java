package com.alphatica.alis.trading.account.actions;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.Account;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static java.lang.String.format;

public record Deposit(double cash) implements AccountActionType {

	@Override
	public void doOnAccount(Time time, Account account) {
		account.addCash(cash);
	}

	@Override
	public String toCsv() {
		return format(Locale.US, "Deposit,%.2f", cash);
	}

	@Override
	public String toString() {
		return "Adding cash: " + cash;
	}

	@Override
	public Map<String, String> toAttributes() {
		HashMap<String, String> m = new HashMap<>();
		m.put("Type", "Add cash");
		m.put("Value", format(Locale.US, "%.2f", cash));
		return m;
	}
}

