package com.alphatica.alis.trading.account.scorer;

import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.ReturnRatios;

import java.util.Map;

/** {@link ReturnRatios#calmar} of the account's session returns and max drawdown; 0 when undefined. */
public final class CalmarRatio implements AccountScorer {
	@Override
	public double score(Account account, Map<String, Double> customStats) {
		double[] returns = account.getAccountHistory().getSessions().returns();
		return Scores.finiteOrZero(ReturnRatios.calmar(returns, account.getMaxDD()));
	}
}
