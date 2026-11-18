package com.alphatica.alis.trading.account.attribution;

import com.alphatica.alis.trading.account.ReturnRatios;
import com.alphatica.alis.trading.account.ReturnRatios.BenchmarkSessions;

/**
 * Sharpe ratio of the account against the index's, over the sessions of {@link ReturnRatios#versusBenchmark} (with
 * the same risk-free returns). With μ the mean and σ the sample standard deviation of the excess returns of the
 * account (a) and the index (m), the difference is split symmetrically (the same whichever change comes first):
 * <ul>
 *     <li>return effect: {@code (μ_a − μ_m)·½(1/σ_a + 1/σ_m)·√252};</li>
 *     <li>volatility effect: {@code ½(μ_a + μ_m)·(1/σ_a − 1/σ_m)·√252}.</li>
 * </ul>
 * NaN with under 2 sessions or without volatility.
 *
 * @param excessReturn      mean excess return of the account in percent per year
 * @param indexExcessReturn the same of the index
 * @param volatility        standard deviation of the account's excess returns in percent per year
 * @param indexVolatility   the same of the index
 * @param index             Sharpe ratio of the index
 * @param account           Sharpe ratio of the account: the index's plus both effects
 */
public record SharpeAttribution(double excessReturn, double indexExcessReturn, double volatility,
								double indexVolatility, double index, double returnEffect, double volatilityEffect,
								double account) {

	public static SharpeAttribution of(BenchmarkSessions sessions) {
		Moments account = Moments.of(sessions.excessReturns(), sessions.period());
		Moments index = Moments.of(sessions.excessBenchmark(), sessions.period());
		double annual = Math.sqrt(ReturnRatios.SESSIONS_PER_YEAR);
		double excessReturn = account.mean * ReturnRatios.SESSIONS_PER_YEAR * 100;
		double indexExcessReturn = index.mean * ReturnRatios.SESSIONS_PER_YEAR * 100;
		if (!(account.deviation > 0) || !(index.deviation > 0)) {
			return new SharpeAttribution(excessReturn, indexExcessReturn, account.deviation * annual * 100,
					index.deviation * annual * 100, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
		}
		double accountInverse = 1 / account.deviation;
		double indexInverse = 1 / index.deviation;
		return new SharpeAttribution(excessReturn, indexExcessReturn, account.deviation * annual * 100,
				index.deviation * annual * 100, index.mean * indexInverse * annual,
				(account.mean - index.mean) * (accountInverse + indexInverse) / 2 * annual,
				(account.mean + index.mean) * (accountInverse - indexInverse) / 2 * annual,
				account.mean * accountInverse * annual);
	}

	/** Mean and sample standard deviation of the sessions with a beta period; NaN under 2 sessions. */
	private record Moments(double mean, double deviation) {

		static Moments of(double[] values, int[] period) {
			double sum = 0;
			int count = 0;
			for (int i = 0; i < values.length; i++) {
				if (period[i] >= 0) {
					sum += values[i];
					count++;
				}
			}
			double mean = sum / count;
			double squares = 0;
			for (int i = 0; i < values.length; i++) {
				if (period[i] >= 0) {
					squares += (values[i] - mean) * (values[i] - mean);
				}
			}
			return new Moments(mean, count < 2 ? Double.NaN : Math.sqrt(squares / (count - 1)));
		}
	}
}
