package com.alphatica.alis.trading.account.actions;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.Account;

import java.util.Locale;

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
}

