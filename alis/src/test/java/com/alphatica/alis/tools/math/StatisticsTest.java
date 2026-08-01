package com.alphatica.alis.tools.math;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StatisticsTest {

	@Test
	void shouldCalculateMedianForList() {
		assertEquals(1.0, Statistics.median(List.of(0.0, 2.0, 1.0)));
		assertEquals(2.0, Statistics.median(List.of(0.0, 1.0, 3.5, 3.0)));
	}

	@Test
	void shouldCalculateMedianForFloatArray() {
		assertEquals(1.0, Statistics.median(new float[] {0.0f, 2.0f, 1.0f}));
		assertEquals(2.0, Statistics.median(new float[] {0.0f, 1.0f, 3.5f, 3.0f}));
	}

	@Test
	void shouldCalculateMedianForDoubleArray() {
		assertEquals(1.0, Statistics.median(new double[] {0.0, 2.0, 1.0}));
		assertEquals(2.0, Statistics.median(new double[] {0.0, 1.0, 3.5, 3.0}));
	}
}
