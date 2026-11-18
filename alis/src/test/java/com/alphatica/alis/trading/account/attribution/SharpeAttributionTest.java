package com.alphatica.alis.trading.account.attribution;

import com.alphatica.alis.data.riskfreerate.RatePeriod;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.ReturnRatios;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.alphatica.alis.trading.account.attribution.AlphaAttributionTest.sessions;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SharpeAttributionTest {
	private static final double DELTA = 1e-9;
	private static final double[] INDEX = {0.01, -0.005, 0.02, 0.002};
	private static final List<Time> TIMES = Arrays.stream(new int[]{20260105, 20260106, 20260107, 20260108})
			.mapToObj(Time::new).toList();
	private static final double RATE = 0.0365;

	@Test
	void shouldAddUpToSharpeOfRatios() {
		double[] account = {0.004, 0.003, 0.012, -0.002};

		SharpeAttribution sharpe = SharpeAttribution.of(sessions(account, INDEX, RATE));

		List<RatePeriod> periods = List.of(new RatePeriod(TIMES.getFirst(), TIMES.getLast(), RATE));
		assertEquals(ReturnRatios.sharpe(account, TIMES, periods), sharpe.account(), DELTA);
		assertEquals(ReturnRatios.sharpe(INDEX, TIMES, periods), sharpe.index(), DELTA);
		assertEquals(sharpe.account(), sharpe.index() + sharpe.returnEffect() + sharpe.volatilityEffect(), DELTA);
		// Per year: 252 · μ over √252 · σ
		assertEquals(sharpe.excessReturn() / sharpe.volatility(), sharpe.account(), DELTA);
	}

	@Test
	void shouldCancelLowerReturnWithLowerVolatilityForHalfOfTheIndex() {
		double[] account = Arrays.stream(INDEX).map(change -> change / 2).toArray();

		SharpeAttribution sharpe = SharpeAttribution.of(sessions(account, INDEX, 0));

		assertEquals(sharpe.index(), sharpe.account(), DELTA);
		assertEquals(-0.75 * sharpe.index(), sharpe.returnEffect(), DELTA);
		assertEquals(0.75 * sharpe.index(), sharpe.volatilityEffect(), DELTA);
		assertEquals(sharpe.indexVolatility() / 2, sharpe.volatility(), DELTA);
		assertEquals(sharpe.indexExcessReturn() / 2, sharpe.excessReturn(), DELTA);
	}

	@Test
	void shouldBeUnknownWithoutVolatility() {
		SharpeAttribution sharpe = SharpeAttribution.of(sessions(new double[4], INDEX, 0));

		assertEquals(0, sharpe.volatility(), DELTA);
		assertTrue(Double.isNaN(sharpe.account()));
		assertTrue(Double.isNaN(sharpe.returnEffect()));
	}
}
