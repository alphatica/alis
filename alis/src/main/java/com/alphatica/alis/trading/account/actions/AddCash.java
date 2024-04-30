package com.alphatica.alis.trading.account.actions;

import com.alphatica.alis.trading.account.Account;

record AddCash(double cash) implements AccountActionType {

	@Override
	public void doOnAccount(Account account) {
		account.addCash(cash);
	}

	@Override
	public String toString() {
		return "Adding cash: " + cash;
	}
}

