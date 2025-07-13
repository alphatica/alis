package com.alphatica.alis.trading.strategy.optimizer;

import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.optimizer.OptimizerException;
import com.alphatica.alis.trading.optimizer.StrategyOptimizer;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;
import com.alphatica.alis.trading.optimizer.params.BoolParam;
import com.alphatica.alis.trading.optimizer.params.DoubleParam;
import com.alphatica.alis.trading.optimizer.params.IntParam;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Supplier;

import static com.alphatica.alis.trading.optimizer.ParametersSelection.GENETIC;
import static com.alphatica.alis.trading.optimizer.ResultVerifier.NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StrategyOptimizerTests {

	@Test
	void shouldGetAllRequiredStrategies() throws OptimizerException {
		Supplier<Strategy> supplier = () -> new Strategy() {
			@BoolParam
			private Boolean p1;

			@DoubleParam(start = 0, step = 0.2, end = 1)
			private double p2;

			@IntParam(start = 10, step = 1, end = 13)
			private int p3;

			@Override
			public List<Order> afterClose(TimeMarketDataSet data, Account account) {
				return List.of();
			}
		};

		StrategyOptimizer strategyOptimizer = new StrategyOptimizer(supplier, null, null, null, NONE, GENETIC, 0);
//		assertEquals(48, optimizer.getPermutationCount()); // TODO fix
	}
}
