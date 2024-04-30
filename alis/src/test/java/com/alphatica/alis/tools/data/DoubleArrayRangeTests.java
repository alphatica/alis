package com.alphatica.alis.tools.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DoubleArrayRangeTests {

    @Test
    void shouldGiveRange() {
        double[] a = new double[]{0.0, 1.0, 2.0, 3.0};
        DoubleArrayRange r = new DoubleArrayRange(a, 1, 3);
        assertEquals(2, r.size());
        assertEquals(1.0, r.get(0));
        assertEquals(2.0, r.get(1));
    }

    @Test()
    void shouldCheckNegativeRange() {
        double[] a = new double[]{0.0, 1.0, 2.0, 3.0};
        DoubleArrayRange r = new DoubleArrayRange(a, 1, 3);
        assertThrows(IndexOutOfBoundsException.class, () -> r.get(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> r.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> r.get(3));
    }
}
