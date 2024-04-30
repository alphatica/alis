package com.alphatica.alis.trading.account;

import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.PositionStats;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class LongPositionTests {

	@Test
	void shouldGetEntryPriceSimple() {
		double price = 10;
		Position position = new Position(new PositionEntry(100, price));
		assertEquals(price, position.getEntryPrice());
	}

	@Test
	void shouldGetEntryPriceAverage() {
		Position position = new Position(new PositionEntry(100, 10));
		position.add(new PositionEntry(150, 20));
		assertEquals(16, position.getEntryPrice());
		assertEquals(250, position.getQuantity());
	}

	@Test
	void shouldReducePositionSimple() {
		Position position = new Position(new PositionEntry(100, 10));
		position.reduce(new PositionExit(60, 1.0));
		assertEquals(10, position.getEntryPrice());
		assertEquals(40, position.getQuantity());
	}

	@Test
	void shouldReducePositionMultiple() {
		Position position = new Position(new PositionEntry(100, 10));
		position.add(new PositionEntry(200, 20));
		position.reduce(new PositionExit(50, 1.0));
		assertEquals(18, position.getEntryPrice());
		assertEquals(250, position.getQuantity());
		position.reduce(new PositionExit(100, 1.0));
		assertEquals(150, position.getQuantity());
		assertEquals(20, position.getEntryPrice());
	}

	@Test
	void shouldThrowWhenReduceTooBig() {
		Position position = new Position(new PositionEntry(100, 10));
		PositionExit exit = new PositionExit(101, 1.0);
		assertThrows(AccountActionException.class, () -> position.reduce(exit));
	}

	@Test
	void shouldUpdateLastPrice() {
		Position position = new Position(new PositionEntry(100, 10));
		position.updateLastPrice(200);
		assertEquals(200, position.getLastPrice());
	}

	@Test
	void shouldCalculateStatsSimple() {
		Position position = new Position(new PositionEntry(100, 10));
		position.reduce(new PositionExit(100, 20));
		PositionStats stats = position.getStats();
		assertEquals(1000, stats.profitValue());
		assertEquals(100, stats.profitPercent());
	}

	@Test
	void shouldCalculateStatsMultipleProfit() {
		Position position = new Position(new PositionEntry(100, 10));
		position.add(new PositionEntry(50, 20));
		position.reduce(new PositionExit(75, 32));
		position.reduce(new PositionExit(75, 20));
		PositionStats stats = position.getStats();
		assertEquals(1900, stats.profitValue());
		assertEquals(95, stats.profitPercent());
	}

	@Test
	void shouldCalculateStatsMultipleLoss() {
		Position position = new Position(new PositionEntry(100, 10));
		position.add(new PositionEntry(50, 20));
		position.reduce(new PositionExit(75, 6));
		position.reduce(new PositionExit(75, 10));
		PositionStats stats = position.getStats();
		assertEquals(-800, stats.profitValue());
		assertEquals(-40, stats.profitPercent());
	}
}
