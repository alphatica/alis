package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.TestData;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.optimizer.params.BoolParam;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;
import com.alphatica.alis.trading.strategy.StrategyExecutor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StrategyOptimizerTest {

	private static final int MAX_OPTIMIZATIONS = 49;

	@Test
	void shouldSelectFuzzyStartTimesAtBeginning() {
		List<Time> selected = FuzzyStartTimeSelector.select(times(1, 100), new Time(1), MAX_OPTIMIZATIONS);

		assertEquals(times(1, 49), selected);
	}

	@Test
	void shouldSelectFuzzyStartTimesInMiddle() {
		List<Time> selected = FuzzyStartTimeSelector.select(times(1, 100), new Time(50), MAX_OPTIMIZATIONS);

		assertEquals(times(26, 74), selected);
	}

	@Test
	void shouldSelectFuzzyStartTimesAtEnd() {
		List<Time> selected = FuzzyStartTimeSelector.select(times(1, 100), new Time(100), MAX_OPTIMIZATIONS);

		assertEquals(times(52, 100), selected);
	}

	@Test
	void shouldUseAllTimesForShortDataSet() {
		List<Time> availableTimes = times(1, 5);

		List<Time> selected = FuzzyStartTimeSelector.select(availableTimes, new Time(3), MAX_OPTIMIZATIONS);

		assertEquals(availableTimes, selected);
	}

	@Test
	void shouldHandleEmptyDataSet() {
		List<Time> selected = FuzzyStartTimeSelector.select(List.of(), new Time(1), MAX_OPTIMIZATIONS);

		assertEquals(List.of(), selected);
	}

	@Test
	void shouldFailOptimizationWhenScoringThrows() throws OptimizerException {
		IllegalStateException failure = new IllegalStateException("scorer failed");
		AtomicInteger scores = new AtomicInteger();
		var optimizer = new StrategyOptimizer(OptimizedStrategy::new, new TestData("market"), StrategyExecutor::new,
				() -> (account, stats) -> {
					throw failure;
				}, ResultVerifier.NONE, ParametersSelection.RANDOM, 1_000);

		OptimizerException exception = assertThrows(OptimizerException.class,
				() -> optimizer.optimize((score, account) -> scores.incrementAndGet()));

		assertSame(failure, exception.getCause());
		assertEquals(0, scores.get());
	}

	@Test
	void shouldScoreEachParamsOnceWithGeneticSelection() throws OptimizerException {
		AtomicInteger scores = new AtomicInteger();
		var optimizer = new StrategyOptimizer(OptimizedStrategy::new, new TestData("market"), StrategyExecutor::new,
				() -> (account, stats) -> stats.get("enabled"), ResultVerifier.NONE, ParametersSelection.GENETIC, 1_000);

		optimizer.optimize((score, account) -> scores.incrementAndGet());

		assertEquals(2, scores.get());
	}

	@Test
	void shouldScoreSeedsFirstWithinMaxCounter() throws OptimizerException {
		List<Map<String, Object>> scored = Collections.synchronizedList(new ArrayList<>());
		var optimizer = new StrategyOptimizer(OptimizedStrategy::new, new TestData("market"), StrategyExecutor::new,
				() -> (account, stats) -> stats.get("enabled"), ResultVerifier.NONE, ParametersSelection.RANDOM, 1);

		optimizer.optimize((score, account) -> scored.add(score.params()), List.of(Map.of("enabled", false)));

		assertEquals(List.of(Map.of("enabled", false)), scored);
		assertEquals(1, optimizer.getLoopCount());
	}

	@Test
	void shouldNotGiveSeedsToGeneticSelection() throws OptimizerException {
		List<Map<String, Object>> scored = Collections.synchronizedList(new ArrayList<>());
		var optimizer = new StrategyOptimizer(OptimizedStrategy::new, new TestData("market"), StrategyExecutor::new,
				() -> (account, stats) -> stats.get("enabled"), ResultVerifier.NONE, ParametersSelection.GENETIC, 1_000);

		optimizer.optimize((score, account) -> scored.add(score.params()), List.of(Map.of("enabled", true)));

		assertEquals(2, Collections.frequency(scored, Map.of("enabled", true)));
		assertEquals(1, Collections.frequency(scored, Map.of("enabled", false)));
		assertEquals(3, scored.size());
	}

	private static final class OptimizedStrategy extends Strategy {
		@BoolParam
		private boolean enabled;

		@Override
		public List<Order> afterClose(TimeMarketDataSet data, Account account) {
			return List.of();
		}

		@Override
		public Map<String, Double> getCustomStats() {
			return Map.of("enabled", enabled ? 1.0 : 0.0);
		}
	}

	private static List<Time> times(int first, int last) {
		return IntStream.rangeClosed(first, last).mapToObj(Time::new).toList();
	}
}
