package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.tools.data.TestMarket;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class BenchmarkReturnsTest {
	private static final MarketName BENCHMARK = new MarketName("benchmark");

	@Test
	void shouldTakeCloseOverPreviousCloseAtEverySession() {
		Market benchmark = TestMarket.withCloses(BENCHMARK, new int[]{20261016, 20261019, 20261020, 20261021},
				new float[]{100, 102, 99, 101});

		double[] returns = BenchmarkReturns.at(benchmark, times(20261019, 20261021));

		assertArrayEquals(new double[]{102.0 / 100 - 1, 101.0 / 99 - 1}, returns, 1e-12);
	}

	@Test
	void shouldBeNanWithoutQuoteOrWithoutPreviousQuote() {
		Market benchmark = TestMarket.withCloses(BENCHMARK, new int[]{20261016, 20261020}, new float[]{100, 102});

		double[] returns = BenchmarkReturns.at(benchmark, times(20261016, 20261019, 20261020));

		assertArrayEquals(new double[]{Double.NaN, Double.NaN, 102.0 / 100 - 1}, returns, 1e-12);
	}

	private static List<Time> times(int... times) {
		return Arrays.stream(times).mapToObj(Time::new).toList();
	}
}
