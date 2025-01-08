package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.PositionStats;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class LongPositionTests {
	private final Time time = new Time(0);
	private final MarketName marketName = new MarketName("market");

	@Test
	void shouldGetEntryPriceSimple() {
		double price = 10;
		Position position = new Position(marketName, new PositionEntry(time, 100, price));
		assertEquals(price, position.getEntryPrice());
	}

	@Test
	void shouldGetEntryPriceAverage() {
		Position position = new Position(marketName, new PositionEntry(time, 100, 10));
		position.add(new PositionEntry(time, 150, 20));
		assertEquals(16, position.getEntryPrice());
		assertEquals(250, position.getQuantity());
	}

	@Test
	void shouldReducePositionSimple() throws AccountActionException {
		Position position = new Position(marketName, new PositionEntry(time, 100, 10));
		position.reduce(new PositionExit(time, 60, 1.0));
		assertEquals(10, position.getEntryPrice());
		assertEquals(40, position.getQuantity());
	}

	@Test
	void shouldReducePositionMultiple() throws AccountActionException {
		Position position = new Position(marketName, new PositionEntry(time, 100, 10));
		position.add(new PositionEntry(time, 200, 20));
		position.reduce(new PositionExit(time, 50, 1.0));
		assertEquals(18, position.getEntryPrice());
		assertEquals(250, position.getQuantity());
		position.reduce(new PositionExit(time, 100, 1.0));
		assertEquals(150, position.getQuantity());
		assertEquals(20, position.getEntryPrice());
	}

	@Test
	void shouldThrowWhenReduceTooBig() {
		Position position = new Position(marketName, new PositionEntry(time, 100, 10));
		PositionExit exit = new PositionExit(time, 101, 1.0);
		assertThrows(AccountActionException.class, () -> position.reduce(exit));
	}

	@Test
	void shouldUpdatePrices() {
		Position position = new Position(marketName, new PositionEntry(time, 100, 10));
		position.updatePrices(200, 200, 200);
		assertEquals(200, position.getLastClose());
	}

	@Test
	void shouldCalculateStatsSimple() throws AccountActionException {
		Position position = new Position(marketName, new PositionEntry(time, 100, 10));
		position.reduce(new PositionExit(time, 100, 20));
		PositionStats stats = position.getStats();
		assertEquals(1000, stats.profitValue());
		assertEquals(100, stats.profitPercent());
	}

	@Test
	void shouldCalculateStatsMultipleProfit() throws AccountActionException {
		Position position = new Position(marketName, new PositionEntry(time, 100, 10));
		position.add(new PositionEntry(time, 50, 20));
		position.reduce(new PositionExit(time, 75, 32));
		position.reduce(new PositionExit(time, 75, 20));
		PositionStats stats = position.getStats();
		assertEquals(1900, stats.profitValue());
		assertEquals(95, stats.profitPercent());
	}

	@Test
	void shouldCalculateStatsMultipleLoss() throws AccountActionException {
		Position position = new Position(marketName, new PositionEntry(time, 100, 10));
		position.add(new PositionEntry(time, 50, 20));
		position.reduce(new PositionExit(time, 75, 6));
		position.reduce(new PositionExit(time, 75, 10));
		PositionStats stats = position.getStats();
		assertEquals(-800, stats.profitValue());
		assertEquals(-40, stats.profitPercent());
	}

	@Test
	void shouldGivePriceRecordSimple() throws AccountActionException {
		Position position = new Position(marketName, new PositionEntry(time, 100, 10));
		position.updatePrices(20, 25, 7);
		List<PositionPricesRecord> records = position.reduce(new PositionExit(time, 100, 20));
		assertEquals(1, records.size());
		PositionPricesRecord first = records.getFirst();
		assertEquals(new PositionPricesRecord(marketName, time, time, 10, 20, 100, 7, 25), first);
	}

	@Test
	void shouldCalcEfficiency() {
		PositionPricesRecord pricesRecord = new PositionPricesRecord(marketName, time, time, 10, 20, 100, 7, 25);
		assertEquals(0.7, pricesRecord.getEntryEfficiency());
		assertEquals(0.8, pricesRecord.getExitEfficiency());
		assertEquals(1_000.0, pricesRecord.getProfitCash());
		assertEquals(100.0, pricesRecord.getProfitPercent());
	}

}
