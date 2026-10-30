package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RunDefaultsTest {

	@Test
	void shouldStartAtFirstSessionOfYearTenYearsBeforeLastSession() {
		RunDefaults defaults = RunDefaults.of(data(20141230, 20151230, 20160104, 20160105, 20261029));

		assertEquals(new RunDefaults("20160104", "20261029", "0.01", "100000"), defaults);
	}

	@Test
	void shouldStartAtFirstSessionWhenDataIsShorter() {
		assertEquals("20180102", RunDefaults.of(data(20180102, 20261029)).startTime());
		assertEquals("20250102", RunDefaults.of(data(20250102)).startTime());
	}

	@Test
	void shouldHaveEmptyTimesWithoutData() {
		assertEquals(new RunDefaults("", "", "0.01", "100000"), RunDefaults.of(null));
		assertEquals(new RunDefaults("", "", "0.01", "100000"), RunDefaults.of(data()));
	}

	private static MarketData data(int... times) {
		List<Time> sessions = Arrays.stream(times).mapToObj(Time::new).toList();
		return new MarketData() {
			@Override
			public List<Time> getTimes() {
				return sessions;
			}

			@Override
			public Market getMarket(MarketName marketName) {
				return null;
			}

			@Override
			public List<Market> listMarkets(Predicate<Market> filter) {
				return List.of();
			}
		};
	}
}
