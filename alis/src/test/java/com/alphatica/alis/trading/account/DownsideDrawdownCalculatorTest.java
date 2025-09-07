package com.alphatica.alis.trading.account;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DownsideDrawdownCalculatorTest {

	@Test
	void doesNotAddLosersWhenOpenProfitGrows() {
		var c = new DownsideDrawDownCalc(100);
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		c.updateState(90, 10);
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		c.updateState(80, 20);
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		c.updateState(70, 40);
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(0, c.getMaxDownsideDrawdown(), 0.01);
		c.updateState(110, 0);
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(0, c.getMaxDownsideDrawdown(), 0.01);
	}

	@Test
	void addLosersWhenNoOpenProfit() {
		var c = new DownsideDrawDownCalc(100);
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		c.updateState(90, 0);
		assertEquals(-10, c.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(-10, c.getMaxDownsideDrawdown(), 0.01);
		c.updateState(80, 0);
		assertEquals(-20, c.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(-20, c.getMaxDownsideDrawdown(), 0.01);
		c.updateState(70, 0);
		assertEquals(-30, c.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(-30, c.getMaxDownsideDrawdown(), 0.01);
		c.updateState(70, 10);
		assertEquals(-20, c.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(-30, c.getMaxDownsideDrawdown(), 0.01);
		c.updateState(70, 40);
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(-30, c.getMaxDownsideDrawdown(), 0.01);
		c.updateState(110, 0);
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(-30, c.getMaxDownsideDrawdown(), 0.01);
	}

	@Test
	void whenNoLosses() {
		var c = new DownsideDrawDownCalc(100);
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		c.updateState(110, 10);
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(0, c.getMaxDownsideDrawdown(), 0.01);
	}

	@Test
	void whenNoProfits() {
		var c = new DownsideDrawDownCalc(100);
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		c.updateState(90, 0);
		assertEquals(-10, c.getCurrentDownsideDrawdown(), 0.01);
		c.updateState(90, -10);
		assertEquals(-20, c.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(-20, c.getMaxDownsideDrawdown(), 0.01);
	}

	@Test
	void whenOpenProfitGreaterThanClosedLoss() {
		var c = new DownsideDrawDownCalc(100);
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		c.updateState(90, 15);
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(0, c.getMaxDownsideDrawdown(), 0.01);
	}

	@Test
	void whenOpenProfitLessThanClosedLoss() {
		var c = new DownsideDrawDownCalc(100);
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		c.updateState(90, 5);
		assertEquals(-5, c.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(-5, c.getMaxDownsideDrawdown(), 0.01);
	}

	@Test
	void whenClosedProfitGreaterThanOpenLoss() {
		var c = new DownsideDrawDownCalc(100);
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		c.updateState(110, -5);
		assertEquals(-4.54, c.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(-4.54, c.getMaxDownsideDrawdown(), 0.01);
	}

	@Test
	void whenClosedProfitLessThanOpenLoss() {
		var c = new DownsideDrawDownCalc(100);
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		c.updateState(105, -10);
		assertEquals(-9.52, c.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(-9.52, c.getMaxDownsideDrawdown(), 0.01);
	}

	@Test
	void whenProfitAndLossUpAndDown() {
		var c = new DownsideDrawDownCalc(100);
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		c.updateState(100, 100); // Large profit
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(0, c.getMaxDownsideDrawdown(), 0.01);
		c.updateState(100, 50); // Falling profit doesn't cause dd
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(0, c.getMaxDownsideDrawdown(), 0.01);
		c.updateState(100, -5); // Open position turned into small loss
		assertEquals(-5, c.getMaxDownsideDrawdown(), 0.01);
		assertEquals(-5, c.getCurrentDownsideDrawdown(), 0.01);
		c.updateState(95, 0); // Closing this position
		assertEquals(-5, c.getMaxDownsideDrawdown(), 0.01);
		assertEquals(-5, c.getCurrentDownsideDrawdown(), 0.01);
		c.updateState(95, 5); // New position brought account back to 100
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(-5, c.getMaxDownsideDrawdown(), 0.01);
		c.updateState(200,0); // Large win closed
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(-5, c.getMaxDownsideDrawdown(), 0.01);
		c.updateState(200, -20); // Another loss opened
		assertEquals(-10, c.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(-10, c.getMaxDownsideDrawdown(), 0.01);
	}

}