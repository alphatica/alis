package com.alphatica.alis.trading.account.attribution;

import com.alphatica.alis.trading.account.ReturnRatios;
import com.alphatica.alis.trading.account.ReturnRatios.BenchmarkSessions;

/**
 * Treynor ratio of {@link ReturnRatios#versusBenchmark} against the index's, in percent per year. The index has beta
 * 1, so its ratio is its mean excess return μ_m. The difference is split symmetrically (the same whichever change
 * comes first), with μ_a the account's mean excess return and β its beta:
 * <ul>
 *     <li>return effect: {@code (μ_a − μ_m)·½(1/β + 1)};</li>
 *     <li>beta effect: {@code ½(μ_a + μ_m)·(1/β − 1)}.</li>
 * </ul>
 * The difference is {@code (μ_a − β·μ_m)/β}: Jensen's alpha over the beta with one beta period; with a beta in
 * every period of the risk-free rate it can differ from that a lot. NaN without a beta.
 */
public record TreynorAttribution(double index, double returnEffect, double betaEffect, double account) {

	public static TreynorAttribution of(BenchmarkSessions sessions) {
		int[] period = sessions.period();
		double excessSum = 0;
		double indexSum = 0;
		int count = 0;
		for (int i = 0; i < period.length; i++) {
			if (period[i] >= 0) {
				excessSum += sessions.excessReturns()[i];
				indexSum += sessions.excessBenchmark()[i];
				count++;
			}
		}
		double scale = (double) ReturnRatios.SESSIONS_PER_YEAR * 100 / count;
		double account = excessSum * scale;
		double index = indexSum * scale;
		double beta = sessions.ratios().beta();
		if (beta == 0) {
			return new TreynorAttribution(index, Double.NaN, Double.NaN, Double.NaN);
		}
		double inverse = 1 / beta;
		return new TreynorAttribution(index, (account - index) * (inverse + 1) / 2, (account + index) * (inverse - 1) / 2,
				account * inverse);
	}
}
