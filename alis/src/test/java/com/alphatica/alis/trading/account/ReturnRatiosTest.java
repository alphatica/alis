package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.riskfreerate.RatePeriod;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.ReturnRatios.BenchmarkRatios;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static java.lang.Double.NaN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReturnRatiosTest {
	private static final double DELTA = 1e-9;
	private static final double[] RETURNS = {0.01, -0.01, 0.02};
	private static final double[] PORTFOLIO = {0.02, -0.01, 0.03, 0.0};
	private static final double[] BENCHMARK = {0.01, -0.005, 0.02, 0.002};
	private static final double SHARPE_OF_RETURNS = 6.92820323027551;
	// 2026-10-23 is a Friday
	private static final List<Time> FRI_MON_TUE = times(20261023, 20261026, 20261027);
	private static final List<Time> FOUR_DAYS = times(20260105, 20260106, 20260107, 20260108);
	/** 1 % per year: 0.0001 per day with 365 days */
	private static final double RATE = 0.0365;

	@Test
	void shouldCalculateSharpeWithoutRiskFreeRate() {
		assertEquals(SHARPE_OF_RETURNS, ReturnRatios.sharpe(RETURNS, FRI_MON_TUE, zeroRate(FRI_MON_TUE)), DELTA);
	}

	@Test
	void shouldSubtractRiskFreeReturnOfCalendarDaysSincePreviousSession() {
		// First session: 1 day; Monday: 3 days since Friday; Tuesday: 1 day
		double[] returns = {0.01 + 0.0001, -0.01 + 0.0003, 0.02 + 0.0001};
		List<RatePeriod> periods = List.of(new RatePeriod(new Time(20261023), new Time(20261027), RATE));

		assertEquals(SHARPE_OF_RETURNS, ReturnRatios.sharpe(returns, FRI_MON_TUE, periods), DELTA);
		assertEquals(18.33030277982336, ReturnRatios.sortino(returns, FRI_MON_TUE, periods), DELTA);
	}

	@Test
	void shouldUseRateInForceAtPreviousSession() {
		// The rate changes on Monday: Monday's return started on Friday, so it still earns the old rate
		List<RatePeriod> periods = List.of(
				new RatePeriod(new Time(20261023), new Time(20261025), RATE),
				new RatePeriod(new Time(20261026), new Time(20261027), RATE * 2));
		double[] returns = {0.01 + 0.0001, -0.01 + 0.0003, 0.02 + 0.0002};

		assertEquals(SHARPE_OF_RETURNS, ReturnRatios.sharpe(returns, FRI_MON_TUE, periods), DELTA);
	}

	@Test
	void shouldCalculateSortinoFromNegativeReturnsOverAllSessions() {
		assertEquals(18.33030277982336, ReturnRatios.sortino(RETURNS, FRI_MON_TUE, zeroRate(FRI_MON_TUE)), DELTA);
	}

	@Test
	void shouldCalculateBetaAndAlphaWithoutRiskFreeRate() {
		BenchmarkRatios ratios = ReturnRatios.versusBenchmark(PORTFOLIO, BENCHMARK, FOUR_DAYS, zeroRate(FOUR_DAYS));

		assertEquals(1.6726748377793799, ratios.beta(), DELTA);
		assertEquals(-32.52198990627251, ratios.jensenAlphaPercent(), DELTA);
		double meanReturn = Arrays.stream(PORTFOLIO).average().orElseThrow();
		assertEquals(meanReturn * 252 * 100 / 1.6726748377793799, ratios.treynorPercent(), DELTA);
	}

	@Test
	void shouldLowerAlphaByOneMinusBetaTimesRiskFreeRate() {
		// Half of the benchmark's return, the rest in cash earning nothing: no alpha without the risk-free rate,
		// -(1 - beta) * rf with it. Every session earns rf = 0.0001.
		List<RatePeriod> periods = List.of(new RatePeriod(new Time(20260105), new Time(20260108), RATE));
		double[] benchmark = new double[BENCHMARK.length];
		double[] portfolio = new double[BENCHMARK.length];
		for (int i = 0; i < benchmark.length; i++) {
			benchmark[i] = BENCHMARK[i] + 0.0001;
			portfolio[i] = 0.5 * benchmark[i];
		}

		BenchmarkRatios withoutRate = ReturnRatios.versusBenchmark(portfolio, benchmark, FOUR_DAYS, zeroRate(FOUR_DAYS));
		BenchmarkRatios withRate = ReturnRatios.versusBenchmark(portfolio, benchmark, FOUR_DAYS, periods);

		assertEquals(0, withoutRate.jensenAlphaPercent(), DELTA);
		assertEquals(0.5, withRate.beta(), DELTA);
		assertEquals(-0.5 * 0.0001 * 252 * 100, withRate.jensenAlphaPercent(), DELTA);
	}

	@Test
	void shouldEstimateBetaInEveryPeriodOfOneRate() {
		List<Time> times = times(20260105, 20260106, 20260107, 20260108, 20260109, 20260112);
		List<RatePeriod> periods = List.of(
				new RatePeriod(new Time(20260105), new Time(20260107), 0),
				new RatePeriod(new Time(20260108), new Time(20260112), 0));
		// Sessions 0-3 start in the first period (session 3 started on 2026-01-07), sessions 4-5 in the second
		double[] benchmark = {0.01, -0.02, 0.03, 0.01, 0.02, -0.01};
		double[] portfolio = {0.02, -0.04, 0.06, 0.02, 0.5 * 0.02 + 0.001, 0.5 * -0.01 + 0.001};

		BenchmarkRatios ratios = ReturnRatios.versusBenchmark(portfolio, benchmark, times, periods);

		assertEquals(4.0 / 6 * 2 + 2.0 / 6 * 0.5, ratios.beta(), DELTA);
		assertEquals(2.0 / 6 * 0.001 * 252 * 100, ratios.jensenAlphaPercent(), DELTA);
	}

	@Test
	void shouldJoinPeriodWithoutBetaToPreviousOne() {
		List<Time> times = times(20260105, 20260106, 20260107, 20260108, 20260109);
		double[] benchmark = {0.01, 0.01, -0.02, 0.03, 0.02};
		double[] portfolio = {0.02, -0.03, 0.05, 0.0, 0.01};
		List<RatePeriod> onePeriod = List.of(new RatePeriod(new Time(20260105), new Time(20260109), 0));
		// The last session (started on 2026-01-08) is alone in the second period
		List<RatePeriod> shortLast = List.of(
				new RatePeriod(new Time(20260105), new Time(20260107), 0),
				new RatePeriod(new Time(20260108), new Time(20260109), 0));
		// The first two sessions (both started on 2026-01-05) are in the first period, where the benchmark is flat
		List<RatePeriod> shortFirst = List.of(
				new RatePeriod(new Time(20260105), new Time(20260105), 0),
				new RatePeriod(new Time(20260106), new Time(20260109), 0));

		BenchmarkRatios expected = ReturnRatios.versusBenchmark(portfolio, benchmark, times, onePeriod);

		assertRatios(expected, ReturnRatios.versusBenchmark(portfolio, benchmark, times, shortLast));
		assertRatios(expected, ReturnRatios.versusBenchmark(portfolio, benchmark, times, shortFirst));
	}

	@Test
	void shouldSkipSessionsWithoutBenchmark() {
		List<Time> times = times(20260105, 20260106, 20260107, 20260108, 20260109);
		double[] portfolio = {0.02, 0.5, -0.01, 0.03, 0.0};
		double[] benchmark = {0.01, NaN, -0.005, 0.02, 0.002};

		assertRatios(ReturnRatios.versusBenchmark(PORTFOLIO, BENCHMARK, FOUR_DAYS, zeroRate(FOUR_DAYS)),
				ReturnRatios.versusBenchmark(portfolio, benchmark, times, zeroRate(times)));
	}

	@Test
	void shouldHaveBetaOneAndNoAlphaForTheBenchmarkItself() {
		List<RatePeriod> periods = List.of(new RatePeriod(new Time(20260105), new Time(20260108), RATE));

		BenchmarkRatios ratios = ReturnRatios.versusBenchmark(BENCHMARK, BENCHMARK, FOUR_DAYS, periods);

		assertEquals(1, ratios.beta(), DELTA);
		assertEquals(0, ratios.jensenAlphaPercent(), DELTA);
	}

	@Test
	void shouldCalculateCagrAndCalmar() {
		assertEquals(423.3186012703455, ReturnRatios.cagrPercent(RETURNS), 1e-6);
		assertEquals(423.3186012703455 / 20, ReturnRatios.calmar(RETURNS, -20), 1e-6);
	}

	@Test
	void shouldDivideTotalReturnByMaxDrawdown() {
		assertEquals(2.5, ReturnRatios.returnToMaxDrawdown(25, -10), DELTA);
		assertEquals(-0.5, ReturnRatios.returnToMaxDrawdown(-5, -10), DELTA);
		assertEquals(NaN, ReturnRatios.returnToMaxDrawdown(25, 0));
	}

	@Test
	void shouldBeUndefinedWithoutEnoughData() {
		double[] one = {0.01};
		List<Time> oneTime = times(20260105);
		double[] flat = {0.01, 0.01, 0.01};
		List<Time> flatTimes = times(20260105, 20260106, 20260107);
		assertEquals(NaN, ReturnRatios.sharpe(one, oneTime, zeroRate(oneTime)));
		assertEquals(NaN, ReturnRatios.sortino(one, oneTime, zeroRate(oneTime)));
		assertEquals(NaN, ReturnRatios.sharpe(flat, flatTimes, zeroRate(flatTimes)));
		// No losses: no downside deviation
		assertEquals(NaN, ReturnRatios.sortino(flat, flatTimes, zeroRate(flatTimes)));
		assertRatios(new BenchmarkRatios(NaN, NaN, NaN),
				ReturnRatios.versusBenchmark(RETURNS, flat, flatTimes, zeroRate(flatTimes)));
		assertRatios(new BenchmarkRatios(NaN, NaN, NaN),
				ReturnRatios.versusBenchmark(one, one, oneTime, zeroRate(oneTime)));
		assertEquals(NaN, ReturnRatios.cagrPercent(new double[0]));
		assertEquals(NaN, ReturnRatios.calmar(RETURNS, 0));
	}

	@Test
	void shouldRejectInvalidInput() {
		List<Time> twoTimes = times(20260105, 20260106);
		List<RatePeriod> periods = zeroRate(twoTimes);
		assertThrows(IllegalArgumentException.class,
				() -> ReturnRatios.versusBenchmark(new double[2], new double[3], twoTimes, periods));
		assertThrows(IllegalArgumentException.class, () -> ReturnRatios.sharpe(new double[3], twoTimes, periods));
		assertThrows(IllegalArgumentException.class,
				() -> ReturnRatios.sharpe(new double[2], times(20260106, 20260106), periods));
		assertThrows(IllegalArgumentException.class, () -> ReturnRatios.sharpe(new double[2], twoTimes,
				List.of(new RatePeriod(new Time(20260106), new Time(20260106), 0))));
	}

	private static void assertRatios(BenchmarkRatios expected, BenchmarkRatios actual) {
		assertEquals(expected.beta(), actual.beta(), DELTA);
		assertEquals(expected.jensenAlphaPercent(), actual.jensenAlphaPercent(), DELTA);
		assertEquals(expected.treynorPercent(), actual.treynorPercent(), DELTA);
	}

	private static List<RatePeriod> zeroRate(List<Time> times) {
		return List.of(new RatePeriod(times.getFirst(), times.getLast(), 0));
	}

	private static List<Time> times(int... times) {
		return Arrays.stream(times).mapToObj(Time::new).toList();
	}
}
