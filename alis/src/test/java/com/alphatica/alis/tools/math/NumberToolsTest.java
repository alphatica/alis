package com.alphatica.alis.tools.math;

import org.junit.jupiter.api.Test;

import static com.alphatica.alis.tools.java.NumberTools.percentChange;
import static org.junit.jupiter.api.Assertions.assertEquals;

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

}
