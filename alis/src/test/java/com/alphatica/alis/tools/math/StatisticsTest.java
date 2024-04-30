package com.alphatica.alis.tools.math;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatisticsTest {

	@Test
	void shouldCalculateMedian() {
		assertEquals(1.0, Statistics.median(new ArrayList<>(List.of(0.0, 2.0, 1.0))));
		assertEquals(2.0, Statistics.median(new ArrayList<>(List.of(0.0, 1.0, 3.5, 3.0))));
	}
}
