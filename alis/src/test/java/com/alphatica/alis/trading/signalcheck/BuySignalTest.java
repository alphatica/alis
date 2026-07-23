package com.alphatica.alis.trading.signalcheck;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class BuySignalTest {
	@Test
	void shouldValidateValues() {
		assertThrows(IllegalArgumentException.class, () -> new BuySignal(0.0, 1.0));
		assertThrows(IllegalArgumentException.class, () -> new BuySignal(-1.0, 1.0));
		assertThrows(IllegalArgumentException.class, () -> new BuySignal(Double.NaN, 1.0));
		assertThrows(IllegalArgumentException.class, () -> new BuySignal(Double.POSITIVE_INFINITY, 1.0));
		assertThrows(IllegalArgumentException.class, () -> new BuySignal(1.0, Double.NaN));
		assertThrows(IllegalArgumentException.class, () -> new BuySignal(1.0, Double.POSITIVE_INFINITY));
	}
}
