package com.alphatica.alis.trading.account.scorer;

import com.alphatica.alis.trading.account.Account;

import java.util.Map;

public class Expectancy implements AccountScorer {
	@Override
	public double score(Account account, Map<String, Double> customStats) {
		return account.getAccountHistory().getStats().expectancy();
	}
}
