package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PositionHistoryRecordTest {
	private static final double DELTA = 0.000_001;
	private final MarketName market = new MarketName("market");
	private final Account account = new Account(100_000);

	@Test
	void shouldMergeManyBuysAndOneSellIntoOneRecord() throws AccountActionException {
		buy(1, 100, 10, 10);
		buy(2, 100, 20, 30);
		sell(3, 200, 30, 12);

		List<PositionHistoryRecord> records = account.getAllHistoryRecords();

		assertEquals(1, records.size());
		PositionHistoryRecord r = records.getFirst();
		assertFalse(r.open());
		assertEquals(new Time(1), r.entryTime());
		assertEquals(new Time(3), r.exitTime());
		assertEquals(15, r.entry(), DELTA);
		assertEquals(30, r.exit(), DELTA);
		assertEquals(200, r.quantity());
		assertEquals(52, r.commission(), DELTA);
		assertEquals(3000, r.getProfitCash(), DELTA);
		assertEquals(100, r.getProfitPercent(), DELTA);
		assertEquals(1, account.getAccountHistory().getHistoryRecords().size());
	}

	@Test
	void shouldMergeMismatchedBuysAndSellsIntoOneRecord() throws AccountActionException {
		buy(1, 100, 10, 0);
		buy(2, 100, 20, 0);
		sell(3, 50, 30, 0);
		sell(4, 120, 40, 0);
		sell(5, 30, 50, 0);

		List<PositionHistoryRecord> records = account.getAllHistoryRecords();

		assertEquals(1, records.size());
		PositionHistoryRecord r = records.getFirst();
		assertEquals(new Time(1), r.entryTime());
		assertEquals(new Time(5), r.exitTime());
		assertEquals(15, r.entry(), DELTA);
		assertEquals(39, r.exit(), DELTA);
		assertEquals(200, r.quantity());
		assertEquals(4, account.getClosedPricesRecords().size());
	}

	@Test
	void shouldSplitPartiallyClosedPositionIntoClosedAndOpenRecords() throws AccountActionException {
		buy(1, 100, 10, 10);
		buy(2, 100, 20, 30);
		sell(3, 150, 30, 12);
		account.getPosition(market).updatePrices(25, 26, 24);

		List<PositionHistoryRecord> records = account.getAllHistoryRecords();

		assertEquals(2, records.size());
		PositionHistoryRecord closed = records.get(0);
		assertFalse(closed.open());
		assertEquals(new Time(1), closed.entryTime());
		assertEquals(new Time(3), closed.exitTime());
		assertEquals(150, closed.quantity());
		assertEquals(2000.0 / 150, closed.entry(), DELTA);
		assertEquals(30, closed.exit(), DELTA);
		assertEquals(42, closed.commission(), DELTA);

		PositionHistoryRecord open = records.get(1);
		assertTrue(open.open());
		assertEquals(new Time(2), open.entryTime());
		assertNull(open.exitTime());
		assertEquals(50, open.quantity());
		assertEquals(20, open.entry(), DELTA);
		assertEquals(25, open.exit(), DELTA);
		assertEquals(10, open.commission(), DELTA);

		assertEquals(account.getAccountHistory().getPaidCommissions(), closed.commission() + open.commission(), DELTA);
		assertTrue(account.getAccountHistory().getHistoryRecords().isEmpty());
	}

	@Test
	void shouldReturnOnlyOpenRecordWhenNothingSold() throws AccountActionException {
		buy(1, 100, 10, 5);
		buy(2, 100, 20, 5);

		List<PositionHistoryRecord> records = account.getAllHistoryRecords();

		assertEquals(1, records.size());
		PositionHistoryRecord open = records.getFirst();
		assertTrue(open.open());
		assertEquals(new Time(1), open.entryTime());
		assertEquals(200, open.quantity());
		assertEquals(15, open.entry(), DELTA);
		assertEquals(10, open.commission(), DELTA);
	}

	@Test
	void shouldCalculateClosedEfficiencyFromFirstBuyAndFirstSell() throws AccountActionException {
		buy(1, 100, 10, 0);
		account.getPosition(market).updatePrices(12, 15, 8);
		buy(2, 100, 12, 0);
		account.getPosition(market).updatePrices(14, 20, 9);
		sell(3, 100, 18, 0);
		account.getPosition(market).updatePrices(20, 25, 5);
		sell(4, 100, 22, 0);

		PositionHistoryRecord r = account.getAllHistoryRecords().getFirst();

		assertEquals(5.0 / 10, r.entryEfficiency(), DELTA);
		assertEquals(18.0 / 25, r.exitEfficiency(), DELTA);
	}

	@Test
	void shouldCalculateEfficiencyOfPartiallyClosedPosition() throws AccountActionException {
		buy(1, 100, 10, 0);
		account.getPosition(market).updatePrices(12, 15, 8);
		buy(2, 100, 12, 0);
		account.getPosition(market).updatePrices(14, 20, 11);
		sell(3, 150, 18, 0);
		account.getPosition(market).updatePrices(19, 24, 16);

		List<PositionHistoryRecord> records = account.getAllHistoryRecords();

		PositionHistoryRecord closed = records.get(0);
		assertEquals(8.0 / 10, closed.entryEfficiency(), DELTA);
		assertEquals(18.0 / 20, closed.exitEfficiency(), DELTA);
		PositionHistoryRecord open = records.get(1);
		assertEquals(11.0 / 12, open.entryEfficiency(), DELTA);
		assertEquals(19.0 / 24, open.exitEfficiency(), DELTA);
	}

	@Test
	void shouldStartNewRecordAfterPositionReopened() throws AccountActionException {
		buy(1, 100, 10, 0);
		sell(2, 100, 12, 0);
		buy(3, 50, 20, 0);
		sell(4, 50, 18, 0);

		List<PositionHistoryRecord> records = account.getAllHistoryRecords();

		assertEquals(2, records.size());
		assertEquals(new Time(1), records.get(0).entryTime());
		assertEquals(12, records.get(0).exit(), DELTA);
		assertEquals(new Time(3), records.get(1).entryTime());
		assertEquals(18, records.get(1).exit(), DELTA);
	}

	@Test
	void shouldCountTradeLengthFromFirstBuyToLastSellAndForOpenPartFromOldestRemainingLot() throws AccountActionException {
		buy(1, 100, 10, 0);
		updatePrices();
		updatePrices();
		buy(2, 100, 10, 0);
		updatePrices();
		sell(3, 150, 12, 0);
		updatePrices();

		List<PositionHistoryRecord> records = account.getAllHistoryRecords();

		assertEquals(3, records.get(0).tradeLength());
		assertEquals(2, records.get(1).tradeLength());
	}

	@Test
	void shouldIncludeClosedPartOfOpenPositionInStats() throws AccountActionException {
		MarketName other = new MarketName("other");
		buy(1, 100, 10, 0);
		buy(2, 100, 20, 0);
		sell(3, 200, 30, 0);
		account.addPosition(other, new PositionEntry(new Time(1), 100, 10), 0);
		account.reducePosition(other, new PositionExit(new Time(2), 50, 5), 0);

		TradeStats stats = account.getStats();

		assertEquals(2, stats.trades());
		assertEquals(50, stats.accuracy(), DELTA);
		assertEquals(100, stats.averageWinPercent(), DELTA);
		assertEquals(-50, stats.averageLossPercent(), DELTA);
		assertEquals(1, account.countProfitableMarkets());
		assertEquals(1, account.countUnprofitableMarkets());
		assertEquals("market 100 %", account.biggestWin());
		assertEquals("other -50 %", account.biggestLoss());
		assertEquals(1, account.getAllStats().get(other).trades());
	}

	@Test
	void shouldNotCountOpenPositionWithoutSellsInStats() throws AccountActionException {
		buy(1, 100, 10, 0);

		assertEquals(0, account.getStats().trades());
	}

	@Test
	void shouldCountPartiallyClosedAndThenClosedPositionAsOneTrade() throws AccountActionException {
		buy(1, 100, 10, 0);
		sell(2, 50, 20, 0);
		assertEquals(1, account.getStats().trades());
		sell(3, 50, 30, 0);

		TradeStats stats = account.getStats();

		assertEquals(1, stats.trades());
		assertEquals(150, stats.averageWinPercent(), DELTA);
	}

	private void updatePrices() {
		account.getPosition(market).updatePrices(10, 10, 10);
	}

	private void buy(int time, int quantity, double price, double commission) throws AccountActionException {
		account.addPosition(market, new PositionEntry(new Time(time), quantity, price), commission);
	}

	private void sell(int time, int quantity, double price, double commission) throws AccountActionException {
		account.reducePosition(market, new PositionExit(new Time(time), quantity, price), commission);
	}
}
