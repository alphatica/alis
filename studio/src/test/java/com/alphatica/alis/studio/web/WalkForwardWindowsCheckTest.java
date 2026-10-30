package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.logic.trading.walkforward.WalkForwardWindows.Window;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalkForwardWindowsCheckTest {
	private static final MarketData DATA = data(20150102, 20200102, 20231229);

	@Test
	void shouldLeaveOutTestWindowsAfterData() {
		// Test windows of 2020, 2021, 2022, 2023, 2024: the data ends in 2023
		List<Window> windows = WalkForwardApi.windowsOf(DATA, new Time(20200101), new Time(20241231), "36", "12");

		assertEquals(List.of(new Time(20200101), new Time(20210101), new Time(20220101), new Time(20230101)),
				windows.stream().map(Window::testStart).toList());
		assertEquals(new Time(20170101), windows.getFirst().trainingStart());
	}

	@Test
	void shouldNeedDataFromFirstTrainingDay() {
		ValidationException error = assertThrows(ValidationException.class,
				() -> WalkForwardApi.windowsOf(DATA, new Time(20170101), new Time(20201231), "36", "12"));

		assertEquals("The first training window starts on 20140101, the data on 20150102: start later or train on fewer months",
				error.getMessage());
		// 24 months from 2015-01-01: the data starts the next day, a holiday
		assertThrows(ValidationException.class,
				() -> WalkForwardApi.windowsOf(DATA, new Time(20170101), new Time(20201231), "24", "12"));
		assertEquals(4, WalkForwardApi.windowsOf(DATA, new Time(20170102), new Time(20201231), "24", "12").size());
	}

	@Test
	void shouldRejectInvalidWindows() {
		assertThrows(ValidationException.class, () -> WalkForwardApi.windowsOf(DATA, new Time(20200101), new Time(20201231), "0", "12"));
		assertThrows(ValidationException.class, () -> WalkForwardApi.windowsOf(DATA, new Time(20200101), new Time(20201231), "12", "601"));
		assertThrows(ValidationException.class, () -> WalkForwardApi.windowsOf(DATA, new Time(20200101), new Time(20201231), "1.5", "12"));
		assertThrows(ValidationException.class, () -> WalkForwardApi.windowsOf(DATA, new Time(20201231), new Time(20200101), "12", "12"));
		ValidationException afterData = assertThrows(ValidationException.class,
				() -> WalkForwardApi.windowsOf(DATA, new Time(20240101), new Time(20241231), "12", "12"));
		assertEquals("The data ends on 20231229, before the first test window", afterData.getMessage());
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
