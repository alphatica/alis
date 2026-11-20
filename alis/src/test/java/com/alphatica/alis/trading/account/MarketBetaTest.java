package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.tools.data.TestMarket;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketBetaTest {
	private static final int SESSIONS = 40;

	@Test
	void shouldBeRatioOfReturnsWhenMarketMovesWithBenchmark() {
		float[] benchmarkCloses = new float[SESSIONS];
		float[] closes = new float[SESSIONS];
		benchmarkCloses[0] = 100;
		closes[0] = 100;
		for (int i = 1; i < SESSIONS; i++) {
			double r = i % 2 == 0 ? 0.01 : -0.008;
			benchmarkCloses[i] = (float) (benchmarkCloses[i - 1] * (1 + r));
			closes[i] = (float) (closes[i - 1] * (1 + 2 * r));
		}
		Market benchmark = TestMarket.withCloses(new MarketName("benchmark"), times(SESSIONS), benchmarkCloses);
		Market market = TestMarket.withCloses(new MarketName("market"), times(SESSIONS), closes);

		assertEquals(2, MarketBeta.of(market, benchmark, new Time(SESSIONS)), 1e-4);
	}

	@Test
	void shouldBeNanWithTooFewSessions() {
		float[] closes = new float[10];
		for (int i = 0; i < closes.length; i++) {
			closes[i] = 100 + i % 3;
		}
		Market benchmark = TestMarket.withCloses(new MarketName("benchmark"), times(10), closes);

		assertTrue(Double.isNaN(MarketBeta.of(benchmark, benchmark, new Time(10))));
	}

	private static int[] times(int count) {
		return IntStream.rangeClosed(1, count).toArray();
	}
}
