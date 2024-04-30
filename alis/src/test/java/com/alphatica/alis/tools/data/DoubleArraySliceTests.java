package com.alphatica.alis.tools.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DoubleArraySliceTests {

	@Test
	void shouldGiveRange() {
		double[] a = new double[]{0.0, 1.0, 2.0, 3.0};
		DoubleArraySlice r = new DoubleArraySlice(a, 1);
		assertEquals(3, r.size());
		assertEquals(1.0, r.get(0));
		assertEquals(2.0, r.get(1));
	}

	@Test
	void shouldCheckOutOfRange() {
		double[] a = new double[]{0.0, 1.0, 2.0, 3.0};
		DoubleArraySlice r = new DoubleArraySlice(a, 1);
		assertThrows(IndexOutOfBoundsException.class, () -> r.get(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> r.get(3));
		assertThrows(IndexOutOfBoundsException.class, () -> r.get(4));
	}
}
