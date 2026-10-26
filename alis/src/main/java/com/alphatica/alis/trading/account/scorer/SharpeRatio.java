package com.alphatica.alis.trading.account.scorer;

import com.alphatica.alis.data.riskfreerate.Country;
import com.alphatica.alis.data.riskfreerate.RatePeriod;
import com.alphatica.alis.data.riskfreerate.RiskFreeRates;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.ReturnRatios;

import java.util.List;

/** {@link ReturnRatios#sharpe} of the account's session returns; 0 when undefined. */
public final class SharpeRatio extends RiskFreeRatioScorer {
	public SharpeRatio(RiskFreeRates rates, Country country) {
		super(rates, country);
	}

	@Override
	double ratio(double[] returns, List<Time> times, List<RatePeriod> periods) {
		return ReturnRatios.sharpe(returns, times, periods);
	}
}
