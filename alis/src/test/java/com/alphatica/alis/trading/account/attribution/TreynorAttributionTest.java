package com.alphatica.alis.trading.account.attribution;

import com.alphatica.alis.trading.account.ReturnRatios.BenchmarkRatios;
import com.alphatica.alis.trading.account.ReturnRatios.BenchmarkSessions;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.alphatica.alis.trading.account.attribution.AlphaAttributionTest.sessions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreynorAttributionTest {
	private static final double DELTA = 1e-9;
	private static final double[] INDEX = {0.01, -0.005, 0.02, 0.002};
	private static final double RATE = 0.0365;

	@Test
	void shouldCancelLowerReturnWithLowerBetaForHalfOfTheIndex() {
		double[] account = Arrays.stream(INDEX).map(change -> change / 2).toArray();
		double index = Arrays.stream(INDEX).average().orElseThrow() * 252 * 100;

		TreynorAttribution treynor = TreynorAttribution.of(sessions(account, INDEX, 0));

		assertEquals(index, treynor.index(), DELTA);
		assertEquals(-0.75 * index, treynor.returnEffect(), DELTA);
		assertEquals(0.75 * index, treynor.betaEffect(), DELTA);
		assertEquals(index, treynor.account(), DELTA);
	}

	@Test
	void shouldAddUpToTreynorOfRatios() {
		double[] account = {0.004, 0.003, 0.012, -0.002};
		BenchmarkSessions sessions = sessions(account, INDEX, RATE);
		BenchmarkRatios ratios = sessions.ratios();

		TreynorAttribution treynor = TreynorAttribution.of(sessions);

		assertEquals(ratios.treynorPercent(), treynor.account(), DELTA);
		assertEquals(treynor.account(), treynor.index() + treynor.returnEffect() + treynor.betaEffect(), DELTA);
		// With one beta period the difference is alpha / beta
		assertEquals(ratios.jensenAlphaPercent() / ratios.beta(), treynor.account() - treynor.index(), DELTA);
	}

	@Test
	void shouldBeUnknownWithoutBeta() {
		TreynorAttribution treynor = TreynorAttribution.of(sessions(new double[4], INDEX, 0));

		assertTrue(Double.isNaN(treynor.returnEffect()));
		assertTrue(Double.isNaN(treynor.account()));
		assertEquals(Arrays.stream(INDEX).average().orElseThrow() * 252 * 100, treynor.index(), DELTA);
	}
}
