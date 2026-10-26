package com.alphatica.alis.trading.account.scorer;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.riskfreerate.Country;
import com.alphatica.alis.data.riskfreerate.RatePeriod;
import com.alphatica.alis.data.riskfreerate.RiskFreeRates;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.BenchmarkReturns;
import com.alphatica.alis.trading.account.ReturnRatios;

import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Jensen's alpha of {@link ReturnRatios#versusBenchmark} in percent a year, against the benchmark's quotes at the
 * account's sessions; 0 when undefined.
 */
public final class JensensAlpha extends RiskFreeRatioScorer {
	private final Market benchmark;

	public JensensAlpha(RiskFreeRates rates, Country country, Market benchmark) {
		super(rates, country);
		this.benchmark = requireNonNull(benchmark, "benchmark");
	}

	@Override
	double ratio(double[] returns, List<Time> times, List<RatePeriod> periods) {
		return ReturnRatios.versusBenchmark(returns, BenchmarkReturns.at(benchmark, times), times, periods)
				.jensenAlphaPercent();
	}
}
