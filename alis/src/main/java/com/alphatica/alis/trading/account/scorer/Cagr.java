package com.alphatica.alis.trading.account.scorer;

import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.ReturnRatios;

import java.util.Map;

/** {@link ReturnRatios#cagrPercent} of the account's session returns; 0 when undefined. */
public final class Cagr implements AccountScorer {
	@Override
	public double score(Account account, Map<String, Double> customStats) {
		return Scores.finiteOrZero(ReturnRatios.cagrPercent(account.getAccountHistory().getSessions().returns()));
	}
}
