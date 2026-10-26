package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FractionalSharesTest {

	private static final MarketName MARKET = new MarketName("market");

	@Test
	void shouldCloseFullySoldPositionDespiteFloatingPointSums() throws AccountActionException {
		Account account = new Account(1000);
		account.addPosition(MARKET, new PositionEntry(new Time(1), 0.1, 100), 0);
		account.addPosition(MARKET, new PositionEntry(new Time(2), 0.2, 100), 0);

		account.reducePosition(MARKET, new PositionExit(new Time(3), 0.3, 110), 0);

		assertNull(account.getPosition(MARKET));
		assertEquals(1003, account.getCash(), 1e-9);
		List<PositionHistoryRecord> history = account.getAllHistoryRecords();
		assertEquals(1, history.size());
		assertEquals(0.3, history.getFirst().quantity());
		assertEquals(3, history.getFirst().getProfitCash(), 1e-9);
	}

	@Test
	void shouldRejectSellingMoreThanHeldWithoutChangingPosition() throws AccountActionException {
		Account account = new Account(1000);
		account.addPosition(MARKET, new PositionEntry(new Time(1), 0.5, 100), 0);

		assertThrows(AccountActionException.class,
				() -> account.reducePosition(MARKET, new PositionExit(new Time(2), 0.500001, 100), 0));

		assertEquals(0.5, account.getPosition(MARKET).getQuantity());
		assertEquals(950, account.getCash(), 1e-9);
	}

	@Test
	void shouldSellPartOfFractionalPosition() throws AccountActionException {
		Account account = new Account(1000);
		account.addPosition(MARKET, new PositionEntry(new Time(1), 0.25, 40), 1);
		account.addPosition(MARKET, new PositionEntry(new Time(2), 0.75, 80), 3);

		account.reducePosition(MARKET, new PositionExit(new Time(3), 0.5, 100), 2);

		Position position = account.getPosition(MARKET);
		assertEquals(0.5, position.getQuantity());
		assertEquals(80, position.getEntryPrice(), 1e-9);
		assertEquals(40, position.getOpenEntryValue(), 1e-9);
		// 1000 - 10 - 1 - 60 - 3 + 50 - 2
		assertEquals(974, account.getCash(), 1e-9);
		PositionHistoryRecord closed = position.getClosedHistoryRecord().orElseThrow();
		assertEquals(0.5, closed.quantity());
		// (0.25 * 40 + 0.25 * 80) / 0.5
		assertEquals(60, closed.entry(), 1e-9);
		assertEquals(100, closed.exit(), 1e-9);
		// half of bought quantity was sold: half of entry commissions
		assertEquals(2, closed.entryCommission(), 1e-9);
		PositionHistoryRecord open = position.getOpenHistoryRecord().orElseThrow();
		assertEquals(0.5, open.quantity());
		assertEquals(2, open.entryCommission(), 1e-9);
	}

	@Test
	void shouldValueFractionalPositionAtLastClose() throws AccountActionException {
		Account account = new Account(100);
		account.addPosition(MARKET, new PositionEntry(new Time(1), 0.125, 80), 0);
		account.getPosition(MARKET).updatePrices(96, 96, 96);

		assertEquals(12, account.getPositionValue(MARKET), 1e-9);
		assertEquals(102, account.getNAV(), 1e-9);
	}

	@Test
	void shouldRecordFractionalQuantitiesInPricesRecords() throws AccountActionException {
		Account account = new Account(100);
		account.addPosition(MARKET, new PositionEntry(new Time(1), 1.5, 10), 0);
		account.reducePosition(MARKET, new PositionExit(new Time(2), 0.25, 12), 0);

		List<PositionPricesRecord> records = account.getAllPricesRecords();

		assertEquals(2, records.size());
		assertEquals(0.25, records.get(0).quantity());
		assertEquals(1.25, records.get(1).quantity());
	}

	@Test
	void shouldCloseFractionalPositionsWithCommission() throws AccountActionException {
		Account account = new Account(100);
		account.addPosition(MARKET, new PositionEntry(new Time(1), 2.5, 10), 0);

		account.close(0.01);

		assertTrue(account.getPositions().isEmpty());
		assertEquals(99.75, account.getCash(), 1e-9);
	}

	@Test
	void shouldRejectQuantityRoundingToZero() {
		Time time = new Time(1);
		assertThrows(IllegalArgumentException.class, () -> new PositionEntry(time, 0.0000004, 10));
		assertThrows(IllegalArgumentException.class, () -> new PositionExit(time, 0.0000004, 10));
	}

	@Test
	void shouldRejectSellingFromMissingPositionWithQuantityInMessage() {
		Account account = new Account(1000);

		var exception = assertThrows(AccountActionException.class,
				() -> account.reducePosition(MARKET, new PositionExit(new Time(2), 0.5, 100), 0));

		assertTrue(exception.getMessage().contains("0.5"), exception.getMessage());
	}

	@Test
	void shouldSkipFullySoldLotsInOpenRecords() throws AccountActionException {
		Position position = new Position(MARKET, new PositionEntry(new Time(1), 0.5, 10));
		position.add(new PositionEntry(new Time(2), 0.000001, 20));
		position.add(new PositionEntry(new Time(3), 1, 30));
		position.reduce(new PositionExit(new Time(4), 0.5, 40));

		List<PositionPricesRecord> records = position.getOpenPositionsPricesRecords();

		assertEquals(2, records.size());
		assertEquals(0.000001, records.get(0).quantity());
		assertEquals(new Time(2), records.get(0).entryTime());
		assertEquals(new Time(2), position.getOpenHistoryRecord().orElseThrow().entryTime());
	}

	@Test
	void shouldHaveNoOpenRecordWhenOnlySmallestQuantityWasLeftAndSold() throws AccountActionException {
		Position position = new Position(MARKET, new PositionEntry(new Time(1), 1.000001, 10));
		position.reduce(new PositionExit(new Time(2), 1, 20));

		assertEquals(0.000001, position.getOpenHistoryRecord().orElseThrow().quantity());

		position.reduce(new PositionExit(new Time(3), 0.000001, 20));

		assertTrue(position.getOpenHistoryRecord().isEmpty());
		assertEquals(0, position.getQuantity());
	}
}
