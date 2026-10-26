package com.alphatica.alis.trading.account;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuantitiesTest {

	@Test
	void shouldConvertSharesToNearestUnits() {
		assertEquals(2_500_000L, Quantities.toUnits(2.5));
		assertEquals(100_000L, Quantities.toUnits(0.1));
		assertEquals(300_000L, Quantities.toUnits(0.1 + 0.2));
		assertEquals(1L, Quantities.toUnits(0.0000006));
		assertEquals(0L, Quantities.toUnits(0.0000004));
		assertEquals(0L, Quantities.toUnits(0.0));
		assertEquals(1_000_000_000_000_000_000L, Quantities.toUnits(Quantities.MAX_QUANTITY));
	}

	@Test
	void shouldRejectQuantitiesOutOfRange() {
		assertThrows(IllegalArgumentException.class, () -> Quantities.toUnits(-0.000001));
		assertThrows(IllegalArgumentException.class, () -> Quantities.toUnits(Math.nextUp(Quantities.MAX_QUANTITY)));
		assertThrows(IllegalArgumentException.class, () -> Quantities.toUnits(Double.NaN));
	}

	@Test
	void shouldConvertUnitsToShares() {
		assertEquals(1.234567, Quantities.toShares(1_234_567L));
		assertEquals(7.0, Quantities.toShares(7_000_000L));
	}

	@Test
	void shouldNormalizeToNearestHeldQuantity() {
		assertEquals(0.3, Quantities.normalize(0.1 + 0.2));
		assertEquals(0.000001, Quantities.normalize(0.0000006));
		assertEquals(1.234568, Quantities.normalize(1.2345675001));
	}

	@Test
	void shouldAcceptQuantitiesTheAccountCanHold() {
		assertTrue(Quantities.isValid(Quantities.MIN_QUANTITY));
		assertTrue(Quantities.isValid(1.5));
		assertTrue(Quantities.isValid(0.1 + 0.2));
		assertTrue(Quantities.isValid(Quantities.MAX_QUANTITY));
		assertTrue(Quantities.isValid(4025));
	}

	@Test
	void shouldRejectQuantitiesTheAccountCannotHold() {
		assertFalse(Quantities.isValid(0.0));
		assertFalse(Quantities.isValid(Math.nextDown(Quantities.MIN_QUANTITY)));
		assertFalse(Quantities.isValid(Math.nextUp(Quantities.MAX_QUANTITY)));
		assertFalse(Quantities.isValid(0.1234567));
		assertFalse(Quantities.isValid(1.0000001));
		assertFalse(Quantities.isValid(-1.0));
		assertFalse(Quantities.isValid(Double.NaN));
	}

	@Test
	void shouldRoundDownIgnoringFloatingPointErrors() {
		assertEquals(0.3, Quantities.roundDown(0.1 + 0.2));
		assertEquals(0.3, Quantities.roundDown(0.7 - 0.4));
		assertEquals(1.234567, Quantities.roundDown(1.2345679));
		assertEquals(4.999999, Quantities.roundDown(4.9999999));
		assertEquals(7.9, Quantities.roundDown(7.9));
		assertEquals(0.0, Quantities.roundDown(0.0000009));
	}

	@Test
	void shouldRoundDownNothingToZero() {
		assertEquals(0.0, Quantities.roundDown(0.0));
		assertEquals(0.0, Quantities.roundDown(-3.5));
	}

	@Test
	void shouldCapRoundDownAtMaxQuantity() {
		assertEquals(Quantities.MAX_QUANTITY, Quantities.roundDown(5e12));
	}

	@Test
	void shouldFormatWithoutTrailingZeros() {
		assertEquals("5", Quantities.format(5.0));
		assertEquals("0.125", Quantities.format(0.125));
		assertEquals("10000000", Quantities.format(1e7));
		assertEquals("0.000001", Quantities.format(Quantities.MIN_QUANTITY));
	}

	@Test
	void shouldAcceptSumsWithFewUlpsOfError() {
		// 2 ulps away from 0.47
		assertTrue(Quantities.isValid(0.4700000000000001));
	}
}
