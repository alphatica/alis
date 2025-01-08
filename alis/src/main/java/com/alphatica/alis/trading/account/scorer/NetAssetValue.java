package com.alphatica.alis.trading.account.scorer;

import com.alphatica.alis.trading.account.Account;

import java.util.Map;

public class NetAssetValue implements AccountScorer {
	@Override
	public double score(Account account, Map<String, Double> stats) {
		return account.getNAV();
	}
}
