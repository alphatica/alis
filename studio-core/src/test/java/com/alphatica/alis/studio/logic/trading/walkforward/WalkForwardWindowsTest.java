package com.alphatica.alis.studio.logic.trading.walkforward;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.logic.trading.walkforward.WalkForwardWindows.Window;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalkForwardWindowsTest {

	@Test
	void shouldPlaceTestWindowsOneAfterAnotherWithTrainingBefore() {
		List<Window> windows = WalkForwardWindows.of(new Time(20200115), new Time(20210310), 6, 4);

		assertEquals(List.of(
				new Window(1, new Time(20190715), new Time(20200114), new Time(20200115), new Time(20200514)),
				new Window(2, new Time(20191115), new Time(20200514), new Time(20200515), new Time(20200914)),
				new Window(3, new Time(20200315), new Time(20200914), new Time(20200915), new Time(20210114)),
				// Cut at the end
				new Window(4, new Time(20200715), new Time(20210114), new Time(20210115), new Time(20210310))), windows);
	}

	@Test
	void shouldCountMonthsFromStartAtEndOfMonth() {
		// From 31 January: 28 February is a month later in 2021, 31 March two months later
		List<Window> windows = WalkForwardWindows.of(new Time(20210131), new Time(20210430), 1, 1);

		assertEquals(List.of(new Time(20210131), new Time(20210228), new Time(20210331), new Time(20210430)),
				windows.stream().map(Window::testStart).toList());
		assertEquals(List.of(new Time(20210227), new Time(20210330), new Time(20210429), new Time(20210430)),
				windows.stream().map(Window::testEnd).toList());
		assertEquals(new Time(20201231), windows.getFirst().trainingStart());
	}

	@Test
	void shouldGiveOneWindowForOneDay() {
		assertEquals(List.of(new Window(1, new Time(20190601), new Time(20200531), new Time(20200601), new Time(20200601))),
				WalkForwardWindows.of(new Time(20200601), new Time(20200601), 12, 12));
	}

	@Test
	void shouldRejectInvalidWindows() {
		Time start = new Time(20200101);
		Time end = new Time(20201231);
		assertThrows(IllegalArgumentException.class, () -> WalkForwardWindows.of(start, end, 0, 12));
		assertThrows(IllegalArgumentException.class, () -> WalkForwardWindows.of(start, end, 12, 0));
		assertThrows(IllegalArgumentException.class, () -> WalkForwardWindows.of(start, end, 601, 12));
		assertThrows(IllegalArgumentException.class, () -> WalkForwardWindows.of(start, end, 12, 601));
		assertThrows(IllegalArgumentException.class, () -> WalkForwardWindows.of(end, start, 12, 12));
		assertEquals(1, WalkForwardWindows.of(start, end, 600, 600).size());
	}
}
