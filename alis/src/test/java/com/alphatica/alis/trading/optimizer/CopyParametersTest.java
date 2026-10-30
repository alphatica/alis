package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.trading.optimizer.params.BoolParam;
import com.alphatica.alis.trading.optimizer.params.DoubleParam;
import com.alphatica.alis.trading.optimizer.params.IntParam;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CopyParametersTest {

	@Test
	void shouldSetParameterFieldsOnlyAndNotifyChange() throws IllegalAccessException {
		Tunable tunable = new Tunable();
		tunable.state = 7;

		Optimizer.copyParameters(Map.of("length", 30, "ratio", 0.25, "fast", true, "state", 99), tunable);

		assertEquals(30, tunable.length);
		assertEquals(0.25, tunable.ratio);
		assertEquals(true, tunable.fast);
		// Not a parameter: kept
		assertEquals(7, tunable.state);
		assertEquals(1, tunable.changes);
	}

	private static final class Tunable implements Optimizable {
		@IntParam(start = 5, step = 5, end = 50)
		private int length = 10;
		@DoubleParam(start = 0.1, step = 0.05, end = 0.5)
		private double ratio = 0.1;
		@BoolParam
		private boolean fast;
		private int state;
		private int changes;

		@Override
		public void paramsChanged() {
			changes++;
		}
	}
}
