package com.alphatica.alis.tools.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FloatArraySliceTests {

	@Test
	void shouldGiveRange() {
		float[] a = new float[]{0.0f, 1.0f, 2.0f, 3.0f};
		FloatArraySlice r = new FloatArraySlice(a, 1);
		assertEquals(3, r.size());
		assertEquals(1.0, r.get(0));
		assertEquals(2.0, r.get(1));
	}

	@Test
	void shouldCheckOutOfRange() {
		float[] a = new float[]{0.0f, 1.0f, 2.0f, 3.0f};
		FloatArraySlice r = new FloatArraySlice(a, 1);
		assertThrows(IndexOutOfBoundsException.class, () -> r.get(-1));
		assertThrows(IndexOutOfBoundsException.class, () -> r.get(3));
		assertThrows(IndexOutOfBoundsException.class, () -> r.get(4));
	}
}
