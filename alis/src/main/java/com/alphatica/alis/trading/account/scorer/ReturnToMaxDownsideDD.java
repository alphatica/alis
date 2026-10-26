package com.alphatica.alis.trading.account.scorer;

import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.ReturnRatios;

import java.util.Map;

/**
 * Time-weighted total return over the max downside drawdown ({@link ReturnRatios#returnToMaxDrawdown}), both in
 * percent; 0 when undefined (no sessions or no downside drawdown).
 */
public final class ReturnToMaxDownsideDD implements AccountScorer {
	@Override
	public double score(Account account, Map<String, Double> customStats) {
		double totalReturnPercent = account.getAccountHistory().getSessions().totalReturnPercent();
		return Scores.finiteOrZero(ReturnRatios.returnToMaxDrawdown(totalReturnPercent, account.getMaxDownsideDD()));
	}
}
