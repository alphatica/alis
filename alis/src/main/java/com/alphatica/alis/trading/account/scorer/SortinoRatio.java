package com.alphatica.alis.trading.account.scorer;

import com.alphatica.alis.data.riskfreerate.Country;
import com.alphatica.alis.data.riskfreerate.RatePeriod;
import com.alphatica.alis.data.riskfreerate.RiskFreeRates;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.ReturnRatios;

import java.util.List;

/** {@link ReturnRatios#sortino} of the account's session returns; 0 when undefined. */
public final class SortinoRatio extends RiskFreeRatioScorer {
	public SortinoRatio(RiskFreeRates rates, Country country) {
		super(rates, country);
	}

	@Override
	double ratio(double[] returns, List<Time> times, List<RatePeriod> periods) {
		return ReturnRatios.sortino(returns, times, periods);
	}
}
