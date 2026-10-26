package com.alphatica.alis.trading.account.scorer;

import com.alphatica.alis.data.riskfreerate.Country;
import com.alphatica.alis.data.riskfreerate.RatePeriod;
import com.alphatica.alis.data.riskfreerate.RiskFreeRates;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.SessionHistory;

import java.util.List;
import java.util.Map;

/** Ratio of the account's session returns with the risk-free rate of one country; 0 when undefined. */
abstract class RiskFreeRatioScorer implements AccountScorer {
	private final RiskFreeRates rates;
	private final Country country;

	RiskFreeRatioScorer(RiskFreeRates rates, Country country) {
		this.rates = rates;
		this.country = country;
	}

	@Override
	public final double score(Account account, Map<String, Double> customStats) {
		SessionHistory sessions = account.getAccountHistory().getSessions();
		List<Time> times = sessions.times();
		if (times.isEmpty()) {
			return 0;
		}
		List<RatePeriod> periods = rates.periods(country, times.getFirst(), times.getLast());
		return Scores.finiteOrZero(ratio(sessions.returns(), times, periods));
	}

	abstract double ratio(double[] returns, List<Time> times, List<RatePeriod> periods);
}
