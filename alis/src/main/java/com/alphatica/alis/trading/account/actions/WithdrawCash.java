package com.alphatica.alis.trading.account.actions;

import com.alphatica.alis.trading.account.Account;

public record WithdrawCash(double cash) implements AccountActionType {
	@Override
	public void doOnAccount(Account account) {
		account.reduceCash(cash);
	}

	@Override
	public String toString() {
		return "Withdrawing cash: " + cash;
	}
}
