package com.alphatica.alis.trading.account.attribution;

import com.alphatica.alis.data.riskfreerate.RatePeriod;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.ReturnRatios;
import com.alphatica.alis.trading.account.ReturnRatios.BenchmarkSessions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static java.lang.Double.NaN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlphaAttributionTest {
	private static final double DELTA = 1e-9;
	/** Monday to Thursday: every session is one calendar day */
	private static final List<Time> TIMES = times(20260105, 20260106, 20260107, 20260108);
	/** 1 % per year: 0.0001 per session */
	private static final double RATE = 0.0365;
	/** Percent per year of a mean return per session */
	private static final double YEARLY = 252 * 100;

	@Test
	void shouldGiveAlphaToTimingWhenInvestedOnlyOnRisingDays() {
		double[] index = {0.02, -0.02, 0.02, -0.02};
		double[] exposure = {1, 0, 1, 0};
		double[] account = {0.02, 0, 0.02, 0};

		AlphaAttribution alpha = AlphaAttribution.of(sessions(account, index, 0), exposure);

		assertEquals(0.01 * YEARLY, alpha.timing(), DELTA);
		assertEquals(0, alpha.selection(), DELTA);
		assertEquals(0, alpha.cash(), DELTA);
		assertEquals(alphaOf(account, index, 0), alpha.alpha(), DELTA);
	}

	@Test
	void shouldGiveAlphaToSelectionWhenStocksBeatIndexFullyInvested() {
		double[] index = {0.01, -0.005, 0.02, 0.002};
		double[] account = Arrays.stream(index).map(change -> change + 0.001).toArray();

		AlphaAttribution alpha = AlphaAttribution.of(sessions(account, index, 0), new double[]{1, 1, 1, 1});

		assertEquals(0.001 * YEARLY, alpha.selection(), DELTA);
		assertEquals(0, alpha.timing(), DELTA);
		assertEquals(0, alpha.cash(), DELTA);
	}

	@Test
	void shouldGiveMissedRiskFreeReturnOfCashToCash() {
		double[] index = {0.01, -0.005, 0.02, 0.002};

		AlphaAttribution alpha = AlphaAttribution.of(sessions(new double[4], index, RATE), new double[4]);

		assertEquals(-0.0001 * YEARLY, alpha.cash(), DELTA);
		assertEquals(0, alpha.selection(), DELTA);
		assertEquals(0, alpha.timing(), DELTA);
		assertEquals(0, alpha.indexPart(), DELTA);
		assertEquals(alphaOf(new double[4], index, RATE), alpha.alpha(), DELTA);
	}

	@Test
	void shouldAddUpToAlphaOfRatiosInEveryRatePeriod() {
		List<Time> times = times(20260105, 20260106, 20260107, 20260108, 20260109, 20260112, 20260113, 20260114);
		List<RatePeriod> periods = List.of(
				new RatePeriod(new Time(20260105), new Time(20260107), RATE),
				new RatePeriod(new Time(20260108), new Time(20260114), RATE * 3));
		double[] index = {0.01, -0.02, NaN, 0.015, -0.01, 0.02, 0.005, -0.012};
		double[] account = {0.004, -0.01, 0.03, 0.012, -0.002, 0.009, 0.004, -0.001};
		double[] exposure = {0, 0.5, 0.6, 0.6, 0.9, 0.3, 0.4, 0.8};
		BenchmarkSessions sessions = ReturnRatios.benchmarkSessions(account, index, times, periods);

		AlphaAttribution alpha = AlphaAttribution.of(sessions, exposure);

		assertEquals(ReturnRatios.versusBenchmark(account, index, times, periods).jensenAlphaPercent(), alpha.alpha(),
				DELTA);
		assertEquals(alpha.excessReturn() - alpha.indexPart(), alpha.alpha(), DELTA);
		assertTrue(alpha.cash() < 0);
	}

	@Test
	void shouldBeUnknownWithoutSessionsWithBeta() {
		double[] index = {0.01, 0.01, 0.01, 0.01};

		AlphaAttribution alpha = AlphaAttribution.of(sessions(new double[4], index, 0), new double[4]);

		assertTrue(Double.isNaN(alpha.alpha()));
		assertTrue(Double.isNaN(alpha.selection()));
	}

	static BenchmarkSessions sessions(double[] account, double[] index, double rate) {
		return ReturnRatios.benchmarkSessions(account, index, TIMES, periods(rate));
	}

	static double alphaOf(double[] account, double[] index, double rate) {
		return ReturnRatios.versusBenchmark(account, index, TIMES, periods(rate)).jensenAlphaPercent();
	}

	private static List<RatePeriod> periods(double rate) {
		return List.of(new RatePeriod(TIMES.getFirst(), TIMES.getLast(), rate));
	}

	private static List<Time> times(int... times) {
		return Arrays.stream(times).mapToObj(Time::new).toList();
	}
}
