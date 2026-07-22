package com.alphatica.alis.trading.account;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DownsideDrawdownCalculatorTest {

	@Test
	void doesNotAddLosersWhenOpenProfitGrows() {
		var c = new DownsideDrawdownCalc(100);
		assertDrawdowns(c, 0, 0);

		c.updateState(50, 110);
		assertDrawdowns(c, 0, 0);

		c.updateState(40, 120);
		assertDrawdowns(c, 0, 0);

		c.updateState(30, 130);
		assertDrawdowns(c, 0, 0);

		c.updateState(20, 140);
		assertDrawdowns(c, 0, 0);
	}

	@Test
	void addLosersWhenNoOpenProfit() {
		var c = new DownsideDrawdownCalc(100);
		c.updateState(90, 90);
		assertDrawdowns(c, -10, -10);

		c.updateState(80, 80);
		assertDrawdowns(c, -20, -20);

		c.updateState(70, 70);
		assertDrawdowns(c, -30, -30);
	}

	@Test
	void whenNoLosses() {
		var c = new DownsideDrawdownCalc(100);
		c.updateState(0, 110);
		assertDrawdowns(c, 0, 0);
	}

	@Test
	void whenOpenProfitLessThanClosedLoss() {
		var c = new DownsideDrawdownCalc(100);
		c.updateState(50, 95);
		assertDrawdowns(c, -5, -5);
	}

	@Test
	void whenClosedProfitGreaterThanOpenLoss() {
		var c = new DownsideDrawdownCalc(100);
		c.updateState(110, 105);
		assertDrawdowns(c, -4.54, -4.54);
	}

	@Test
	void whenClosedProfitLessThanOpenLoss() {
		var c = new DownsideDrawdownCalc(100);
		c.updateState(105, 95);
		assertDrawdowns(c, -9.52, -9.52);
	}

	@Test
	void whenProfitAndLossUpAndDown() {
		var c = new DownsideDrawdownCalc(100);
		assertEquals(0, c.getCurrentDownsideDrawdown(), 0.01);
		c.updateState(0, 100); // Large profit
		assertDrawdowns(c, 0, 0);

		c.updateState(0, 150); // Falling profit doesn't cause dd
		assertDrawdowns(c, 0, 0);

		c.updateState(0, 95); // Open position turned into small loss
		assertDrawdowns(c, -5, -5);

		c.updateState(95, 95); // Closing this position
		assertDrawdowns(c, -5, -5);

		c.updateState(50, 100); // New position brought account back to 100
		assertDrawdowns(c, 0, -5);

		c.updateState(200,200); // Large win closed
		assertDrawdowns(c, 0, -5);

		c.updateState(0, 180); // Another loss opened
		assertDrawdowns(c, -10, -10);
	}

	private void assertDrawdowns(DownsideDrawdownCalc drawdownCalc, double current, double max) {
		assertEquals(current, drawdownCalc.getCurrentDownsideDrawdown(), 0.01);
		assertEquals(max, drawdownCalc.getMaxDownsideDrawdown(), 0.01);
	}
}