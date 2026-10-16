package com.alphatica.alis.tools.math;

import org.junit.jupiter.api.Test;

import static com.alphatica.alis.tools.java.NumberTools.assertNotNegative;
import static com.alphatica.alis.tools.java.NumberTools.percentChange;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NumberToolsTest {

	@Test
	void shouldCalculatePercentChangeUp() {
		double now = 2;
		double initial = 1;
		assertEquals(100.0, percentChange(initial, now));
	}

	@Test
	void shouldCalculatePercentChangeDown() {
		double now = 1;
		double initial = 2;
		assertEquals(-50.0, percentChange(initial, now));
	}

	@Test
	void shouldAcceptZeroAndPositiveAsNotNegative() {
		assertDoesNotThrow(() -> assertNotNegative(0.0, 1.0));
	}

	@Test
	void shouldRejectNegativeAndNaNAsNotNegative() {
		assertThrows(IllegalArgumentException.class, () -> assertNotNegative(0.0, -1.0));
		assertThrows(IllegalArgumentException.class, () -> assertNotNegative(Double.NaN));
	}
}
