package com.alphatica.alis.trading.account.attribution;

import com.alphatica.alis.trading.account.ReturnRatios;
import com.alphatica.alis.trading.account.ReturnRatios.BenchmarkSessions;

import java.util.Arrays;

/**
 * Jensen's alpha of {@link ReturnRatios#versusBenchmark} split into stock selection, timing and cash, in percent per
 * year. With e the excess returns of the account (a) and the index (m), x the invested part of the account at the
 * start of the session, rf the risk-free return and β_k the beta of the session's period:
 * <ul>
 *     <li>cash: {@code −(1 − x)·rf}, the cash part earning nothing instead of rf;</li>
 *     <li>selection: {@code y − b_k·x·e_m} with {@code y = e_a − cash}, where b_k is the slope of y on
 *     {@code x·e_m} in the period (the beta of the stocks held, per unit invested): how much the stocks beat the
 *     index at the part invested that session;</li>
 *     <li>timing: {@code b_k·x·e_m − β_k·e_m}: the part invested that session instead of the period's beta.</li>
 * </ul>
 * In every session they add up to {@code e_a − β_k·e_m}, so their means over the sessions add up to the alpha.
 *
 * @param excessReturn mean excess return of the account
 * @param indexPart    mean of β_k·e_m: what the index gives at the account's beta; excess return minus alpha
 */
public record AlphaAttribution(double excessReturn, double indexPart, double selection, double timing, double cash) {

	/**
	 * @param exposureAtStart invested part of the account at the start of every session, e.g.
	 *                        {@link com.alphatica.alis.trading.account.SessionHistory#exposureAtStart()}
	 */
	public static AlphaAttribution of(BenchmarkSessions sessions, double[] exposureAtStart) {
		int[] period = sessions.period();
		if (exposureAtStart.length != period.length) {
			throw new IllegalArgumentException("exposure and sessions must have the same length");
		}
		long count = Arrays.stream(period).filter(p -> p >= 0).count();
		double scale = (double) ReturnRatios.SESSIONS_PER_YEAR * 100 / count;
		double[] slopes = slopes(sessions, exposureAtStart);
		double[] excess = sessions.excessReturns();
		double[] index = sessions.excessBenchmark();
		double[] betas = sessions.periodBetas();
		double excessSum = 0;
		double indexSum = 0;
		double selection = 0;
		double timing = 0;
		double cash = 0;
		for (int i = 0; i < period.length; i++) {
			int p = period[i];
			if (p >= 0) {
				double sessionCash = cash(exposureAtStart[i], sessions.riskFree()[i]);
				double invested = exposureAtStart[i] * index[i];
				excessSum += excess[i];
				indexSum += betas[p] * index[i];
				selection += excess[i] - sessionCash - slopes[p] * invested;
				timing += slopes[p] * invested - betas[p] * index[i];
				cash += sessionCash;
			}
		}
		return new AlphaAttribution(excessSum * scale, indexSum * scale, selection * scale, timing * scale,
				cash * scale);
	}

	public double alpha() {
		return selection + timing + cash;
	}

	private static double cash(double exposure, double riskFree) {
		return -(1 - exposure) * riskFree;
	}

	/** Slope b_k of every period: 0 when the invested index return does not change (e.g. nothing invested). */
	private static double[] slopes(BenchmarkSessions sessions, double[] exposure) {
		int periods = sessions.periodBetas().length;
		double[] sumY = new double[periods];
		double[] sumZ = new double[periods];
		int[] count = new int[periods];
		forEachSession(sessions, exposure, (p, y, z) -> {
			sumY[p] += y;
			sumZ[p] += z;
			count[p]++;
		});
		double[] coMoment = new double[periods];
		double[] moment = new double[periods];
		forEachSession(sessions, exposure, (p, y, z) -> {
			double deltaZ = z - sumZ[p] / count[p];
			coMoment[p] += (y - sumY[p] / count[p]) * deltaZ;
			moment[p] += deltaZ * deltaZ;
		});
		double[] slopes = new double[periods];
		for (int p = 0; p < periods; p++) {
			slopes[p] = moment[p] > 0 ? coMoment[p] / moment[p] : 0;
		}
		return slopes;
	}

	/** y (the excess return without the cash part) and z (the invested part times the index's excess return). */
	private static void forEachSession(BenchmarkSessions sessions, double[] exposure, SessionConsumer consumer) {
		int[] period = sessions.period();
		for (int i = 0; i < period.length; i++) {
			if (period[i] >= 0) {
				double y = sessions.excessReturns()[i] - cash(exposure[i], sessions.riskFree()[i]);
				consumer.accept(period[i], y, exposure[i] * sessions.excessBenchmark()[i]);
			}
		}
	}

	@FunctionalInterface
	private interface SessionConsumer {
		void accept(int period, double y, double z);
	}
}
