package com.alphatica.alis.trading.account.scorer;

import com.alphatica.alis.trading.account.Account;

import java.util.Map;

public class NavAdjustedForMaxDD implements AccountScorer {
	@Override
	public double score(Account account, Map<String, Double> customStats) {
		return Scores.finiteOrZero(account.getNAV() / -account.getMaxDD());
	}
}
