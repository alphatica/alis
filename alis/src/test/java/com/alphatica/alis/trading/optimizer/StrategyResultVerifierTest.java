package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.TestData;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.optimizer.params.BoolParam;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;
import com.alphatica.alis.trading.strategy.StrategyExecutor;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrategyResultVerifierTest {

	@Test
	void shouldExecuteStrategyWithSelectedParameters() throws IllegalAccessException {
		var verifier = new StrategyResultVerifier(OptimizedStrategy::new, new TestData("market"),
				StrategyExecutor::new, () -> (account, stats) -> stats.get("enabled"), exception -> { });

		var result = verifier.verify(ResultVerifier.NONE, Map.of("enabled", true));

		assertTrue(result.isPresent());
		assertEquals(1.0, result.orElseThrow().score());
	}

	@Test
	void shouldReturnMedianScoreForReducedOrdersVerification() throws IllegalAccessException {
		AtomicInteger score = new AtomicInteger();
		var verifier = new StrategyResultVerifier(OptimizedStrategy::new, new TestData("market"),
				StrategyExecutor::new, () -> (account, stats) -> score.incrementAndGet(), exception -> { });

		var result = verifier.verify(ResultVerifier.REMOVE_ORDERS, Map.of("enabled", true));

		assertEquals(49, score.get());
		assertEquals(25.0, result.orElseThrow().score());
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
}
