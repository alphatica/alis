package com.alphatica.alis.trading.strategy.optimizer;

import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;
import com.alphatica.alis.trading.strategy.params.BoolParam;
import com.alphatica.alis.trading.strategy.params.DoubleParam;
import com.alphatica.alis.trading.strategy.params.IntParam;
import com.alphatica.alis.trading.strategy.params.Validator;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ValidatorTests {

	@Test
	void shouldAllowValidateStrategy() throws OptimizerException {
		var s = new Strategy() {
			@BoolParam
			private boolean p1;

			@BoolParam
			private Boolean p2;

			@DoubleParam(start = 0, step = 1, end = 100)
			private double p3;

			@DoubleParam(start = 0, step = 1, end = 100)
			private Double p4;

			@IntParam(start = 0, step = 1, end = 100)
			private int p5;

			@IntParam(start = 0, step = 1, end = 100)
			private Integer p6;

			@Override
			public List<Order> afterClose(TimeMarketDataSet data, Account account) {
				return List.of();
			}
		};
		Validator.validate(s);
	}

	@Test
	void shouldValidateBoolParam() {
		var s = new Strategy() {

			@BoolParam
			private int p1;

			@Override
			public List<Order> afterClose(TimeMarketDataSet data, Account account) {
				return List.of();
			}
		};

		assertThrows(OptimizerException.class, () -> Validator.validate(s));
	}

	@Test
	void shouldValidateIntParam() {
		var s = new Strategy() {

			@IntParam(start = 0, step = 1, end = 100)
			private boolean p1;

			@Override
			public List<Order> afterClose(TimeMarketDataSet data, Account account) {
				return List.of();
			}
		};

		assertThrows(OptimizerException.class, () -> Validator.validate(s));
	}

	@Test
	void shouldValidateIntRange() {
		var s = new Strategy() {

			@IntParam(start = 10, step = 20, end = 20)
			private boolean p1;

			@Override
			public List<Order> afterClose(TimeMarketDataSet data, Account account) {
				return List.of();
			}
		};
		assertThrows(OptimizerException.class, () -> Validator.validate(s));
	}

	@Test
	void shouldValidateDoubleParam() {
		var s = new Strategy() {

			@DoubleParam(start = 0, step = 1, end = 100)
			private int p1;

			@Override
			public List<Order> afterClose(TimeMarketDataSet data, Account account) {
				return List.of();
			}
		};
		assertThrows(OptimizerException.class, () -> Validator.validate(s));
	}

	@Test
	void shouldValidateDoubleRange() {
		var s = new Strategy() {

			@DoubleParam(start = 30, step = 1, end = 20)
			private int p1;

			@Override
			public List<Order> afterClose(TimeMarketDataSet data, Account account) {
				return List.of();
			}
		};
		assertThrows(OptimizerException.class, () -> Validator.validate(s));
	}
}
