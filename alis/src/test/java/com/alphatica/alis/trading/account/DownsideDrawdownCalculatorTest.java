package com.alphatica.alis.trading.account;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DownsideDrawdownCalculatorTest {

	@Test
	void doesNotAddLosersWhenOpenProfitGrows() {
		var c = new DownsideDrawDownCalc(100);
		assertDrawDowns(c, 0, 0);

		c.updateState(50, 110);
		assertDrawDowns(c, 0, 0);

		c.updateState(40, 120);
		assertDrawDowns(c, 0, 0);

		c.updateState(30, 130);
		assertDrawDowns(c, 0, 0);

		c.updateState(20, 140);
		assertDrawDowns(c, 0, 0);
	}

	@Test
	void addLosersWhenNoOpenProfit() {
		var c = new DownsideDrawDownCalc(100);
		c.updateState(90, 90);
		assertDrawDowns(c, -10, -10);

		c.updateState(80, 80);
		assertDrawDowns(c, -20, -20);

		c.updateState(70, 70);
		assertDrawDowns(c, -30, -30);
	}

	@Test
	void whenNoLosses() {
		var c = new DownsideDrawDownCalc(100);
		c.updateState(0, 110);
		assertDrawDowns(c, 0, 0);
	}

	@Test
	void whenOpenProfitLessThanClosedLoss() {
		var c = new DownsideDrawDownCalc(100);
		c.updateState(50, 95);
		assertDrawDowns(c, -5, -5);
	}

	@Test
	void whenClosedProfitGreaterThanOpenLoss() {
		var c = new DownsideDrawDownCalc(100);
		c.updateState(110, 105);
		assertDrawDowns(c, -4.54, -4.54);
	}

	@Test
	void whenClosedProfitLessThanOpenLoss() {
		var c = new DownsideDrawDownCalc(100);
		c.updateState(105, 95);
		assertDrawDowns(c, -9.52, -9.52);
	}

	@Test
	void whenProfitAndLossUpAndDown() {
		var c = new DownsideDrawDownCalc(100);
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		c.updateState(0, 100); // Large profit
		assertDrawDowns(c, 0, 0);

		c.updateState(0, 150); // Falling profit doesn't cause dd
		assertDrawDowns(c, 0, 0);

		c.updateState(0, 95); // Open position turned into small loss
		assertDrawDowns(c, -5, -5);

		c.updateState(95, 95); // Closing this position
		assertDrawDowns(c, -5, -5);

		c.updateState(50, 100); // New position brought account back to 100
		assertDrawDowns(c, 0, -5);

		c.updateState(200,200); // Large win closed
		assertDrawDowns(c, 0, -5);

		c.updateState(0, 180); // Another loss opened
		assertDrawDowns(c, -10, -10);
	}

	private void assertDrawDowns(DownsideDrawDownCalc drawDownCalc, double current, double max) {
		assertEquals(current, drawDownCalc.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(max, drawDownCalc.getMaxDownsideDrawdown(), 0.01);
	}
}