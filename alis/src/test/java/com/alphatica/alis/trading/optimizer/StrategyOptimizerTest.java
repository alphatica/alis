package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.data.time.Time;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StrategyOptimizerTest {

	private static final int MAX_OPTIMIZATIONS = 49;

	@Test
	void shouldSelectFuzzyStartTimesAtBeginning() {
		List<Time> selected = StrategyOptimizer.selectFuzzyStartTimes(times(1, 100), new Time(1), MAX_OPTIMIZATIONS);

		assertEquals(times(1, 49), selected);
	}

	@Test
	void shouldSelectFuzzyStartTimesInMiddle() {
		List<Time> selected = StrategyOptimizer.selectFuzzyStartTimes(times(1, 100), new Time(50), MAX_OPTIMIZATIONS);

		assertEquals(times(26, 74), selected);
	}

	@Test
	void shouldSelectFuzzyStartTimesAtEnd() {
		List<Time> selected = StrategyOptimizer.selectFuzzyStartTimes(times(1, 100), new Time(100), MAX_OPTIMIZATIONS);

		assertEquals(times(52, 100), selected);
	}

	@Test
	void shouldUseAllTimesForShortDataSet() {
		List<Time> availableTimes = times(1, 5);

		List<Time> selected = StrategyOptimizer.selectFuzzyStartTimes(availableTimes, new Time(3), MAX_OPTIMIZATIONS);

		assertEquals(availableTimes, selected);
	}

	@Test
	void shouldHandleEmptyDataSet() {
		List<Time> selected = StrategyOptimizer.selectFuzzyStartTimes(List.of(), new Time(1), MAX_OPTIMIZATIONS);

		assertEquals(List.of(), selected);
	}

	private static List<Time> times(int first, int last) {
		return IntStream.rangeClosed(first, last).mapToObj(Time::new).toList();
	}
}
