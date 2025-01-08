package com.alphatica.alis.trading.account.scorer;

import com.alphatica.alis.trading.account.Account;

import java.util.Map;

public interface AccountScorer {
	double score(Account account, Map<String, Double> customStats);
}
