package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.FloatArraySlice;
import com.alphatica.alis.data.market.Delisting;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionHistoryTest {
	private static final double DELTA = 1e-12;
	private static final MarketName MARKET = new MarketName("market");

	/** 1000 cash, 10 units bought at 50: 500 in cash, 500 in the position. */
	private static Account accountWithPosition() throws AccountActionException {
		Account account = new Account(1000);
		account.addPosition(MARKET, new PositionEntry(new Time(1), 10, 50), 0);
		return account;
	}

	@Test
	void shouldMeasureFirstSessionFromInitialCash() throws AccountActionException {
		Account account = accountWithPosition();

		account.updateLastKnown(prices(55, 1)); // 1050
		account.updateLastKnown(prices(44, 2)); // 940

		assertEquals(List.of(new Time(1), new Time(2)), account.getAccountHistory().getSessions().times());
		assertArrayEquals(new double[]{0.05, 940.0 / 1050 - 1}, account.getAccountHistory().getSessions().returns(), DELTA);
		assertEquals(-6, account.getAccountHistory().getSessions().totalReturnPercent(), DELTA);
	}

	@Test
	void shouldRecordExposureDrawdownsAndPositionsAtEverySession() throws AccountActionException {
		Account account = accountWithPosition();

		account.updateLastKnown(prices(55, 1)); // 1050: 550 in the position
		account.updateLastKnown(prices(44, 2)); // 940: 440 in the position, closed equity 1000

		SessionHistory sessions = account.getAccountHistory().getSessions();
		assertArrayEquals(new double[]{100 * 550.0 / 1050, 100 * 440.0 / 940}, sessions.exposurePercent(), DELTA);
		assertArrayEquals(new double[]{0, 100 * (940.0 / 1050 - 1)}, sessions.drawdownPercent(), DELTA);
		assertArrayEquals(new double[]{0, -6}, sessions.downsideDrawdownPercent(), DELTA);
		assertArrayEquals(new int[]{1, 1}, sessions.positionCount());
	}

	@Test
	void shouldCountDepositAtStartOfNextSession() throws AccountActionException {
		Account account = accountWithPosition();
		account.updateLastKnown(prices(50, 1)); // 1000

		account.addCash(1000);
		account.updateLastKnown(prices(60, 2)); // 2100: +100 on 2000 invested

		assertArrayEquals(new double[]{0, 0.05}, account.getAccountHistory().getSessions().returns(), DELTA);
		assertEquals(5, account.getAccountHistory().getSessions().totalReturnPercent(), DELTA);
	}

	@Test
	void shouldMeasureExposureAtStartWithSessionsDeposits() throws AccountActionException {
		Account account = accountWithPosition();
		account.updateLastKnown(prices(50, 1)); // 500 of 1000 in the position

		account.addCash(1000);
		account.updateLastKnown(prices(60, 2)); // starts with 500 of 2000 in the position

		assertArrayEquals(new double[]{1000, 2000}, account.getAccountHistory().getSessions().startValues(), DELTA);
		assertArrayEquals(new double[]{0, 0.25}, account.getAccountHistory().getSessions().exposureAtStart(), DELTA);
	}

	@Test
	void shouldCountWithdrawalAtStartOfNextSession() throws AccountActionException {
		Account account = accountWithPosition();
		account.updateLastKnown(prices(50, 1)); // 1000

		account.reduceCash(400);
		account.updateLastKnown(prices(56, 2)); // 660: +60 on 600 invested

		assertArrayEquals(new double[]{0, 0.1}, account.getAccountHistory().getSessions().returns(), DELTA);
	}

	@Test
	void shouldValueSameSessionAgain() throws AccountActionException {
		Account account = accountWithPosition();
		account.updateLastKnown(prices(60, 1));

		account.updateLastKnown(prices(40, 1)); // 900

		assertEquals(List.of(new Time(1)), account.getAccountHistory().getSessions().times());
		assertArrayEquals(new double[]{-0.1}, account.getAccountHistory().getSessions().returns(), DELTA);
	}

	@Test
	void shouldKeepDepositWhenSameSessionIsValuedAgain() throws AccountActionException {
		Account account = accountWithPosition();
		account.updateLastKnown(prices(50, 1));
		account.addCash(500);
		account.updateLastKnown(prices(50, 2));

		account.updateLastKnown(prices(65, 2)); // 1650: +150 on 1500 invested

		assertArrayEquals(new double[]{0, 0.1}, account.getAccountHistory().getSessions().returns(), DELTA);
	}

	@Test
	void shouldRevalueLastSessionAfterDelisting() throws AccountActionException {
		Account account = accountWithPosition();
		account.updateLastKnown(prices(50, 1));

		account.processDelistings(List.of(new Delisting(MARKET, new Time(1), 20)), new Time(1)); // 700

		assertArrayEquals(new double[]{-0.3}, account.getAccountHistory().getSessions().returns(), DELTA);
	}

	@Test
	void shouldRevalueLastSessionAfterClosingWithCommission() throws AccountActionException {
		Account account = accountWithPosition();
		account.updateLastKnown(prices(50, 1));

		account.close(0.01); // commission 5

		assertArrayEquals(new double[]{-0.005}, account.getAccountHistory().getSessions().returns(), DELTA);
	}

	@Test
	void shouldLeaveOutWithdrawalAfterLastValuation() throws AccountActionException {
		Account account = accountWithPosition();
		account.updateLastKnown(prices(60, 1)); // 1100
		account.close(0);

		account.reduceCash(1100);

		assertArrayEquals(new double[]{0.1}, account.getAccountHistory().getSessions().returns(), DELTA);
		assertEquals(10, account.getAccountHistory().getSessions().totalReturnPercent(), DELTA);
	}

	@Test
	void shouldCompoundAcrossSessionWithEverythingWithdrawn() throws AccountActionException {
		Account account = new Account(0);
		account.addCash(1000);
		account.addPosition(MARKET, new PositionEntry(new Time(1), 10, 50), 0);
		account.updateLastKnown(prices(50, 1)); // 1000
		account.updateLastKnown(prices(60, 2)); // 1100: +10 %
		account.reducePosition(MARKET, new PositionExit(new Time(3), 10, 60), 0);
		account.reduceCash(1100);
		account.updateLastKnown(prices(60, 3)); // nothing invested: no return
		account.addCash(500);
		account.addPosition(MARKET, new PositionEntry(new Time(4), 10, 50), 0);
		account.updateLastKnown(prices(50, 4)); // 500
		account.updateLastKnown(prices(55, 5)); // 550: +10 %

		assertArrayEquals(new double[]{1000, 1100, 0, 500, 550}, account.getAccountHistory().getSessions().nav(), DELTA);
		assertArrayEquals(new double[]{0, 0.1, 0, 0, 0.1}, account.getAccountHistory().getSessions().returns(), DELTA);
		assertArrayEquals(new double[]{0, 10, 10, 10, 21},
				account.getAccountHistory().getSessions().cumulativeReturnPercent(), 1e-9);
		assertEquals(21, account.getAccountHistory().getSessions().totalReturnPercent(), 1e-9);
	}

	@Test
	void shouldHaveNoReturnWhenNothingIsInvested() {
		Account account = new Account(0);

		account.updateLastKnown(prices(50, 1));

		assertArrayEquals(new double[]{0}, account.getAccountHistory().getSessions().returns(), DELTA);
		assertEquals(0, account.getAccountHistory().getSessions().totalReturnPercent(), DELTA);
	}

	@Test
	void shouldHaveNoTotalReturnWithoutSessions() throws AccountActionException {
		Account account = accountWithPosition();

		account.close(0.01);

		assertTrue(account.getAccountHistory().getSessions().times().isEmpty());
		assertEquals(0, account.getAccountHistory().getSessions().returns().length);
		assertTrue(Double.isNaN(account.getAccountHistory().getSessions().totalReturnPercent()));
	}

	@Test
	void shouldRecordClosedEquity() throws AccountActionException {
		Account account = accountWithPosition();
		account.updateLastKnown(prices(60, 1)); // open profit not counted: 1000
		account.updateLastKnown(prices(40, 2)); // open loss not counted: 1000

		account.addCash(1000);
		account.reducePosition(MARKET, new PositionExit(new Time(3), 10, 45), 0);
		account.updateLastKnown(prices(45, 3)); // loss of 50 realized: 1950 of 2000 invested

		SessionHistory sessions = account.getAccountHistory().getSessions();
		assertArrayEquals(new double[]{1000, 1000, 1950}, sessions.closedEquity(), DELTA);
		assertArrayEquals(new double[]{0, 0, -2.5}, sessions.cumulativeClosedReturnPercent(), 1e-9);
	}

	@Test
	void shouldRejectEarlierSession() {
		Account account = new Account(1000);
		account.updateLastKnown(prices(50, 2));

		assertThrows(IllegalArgumentException.class, () -> account.updateLastKnown(prices(50, 1)));
	}

	@Test
	void shouldKeepManySessions() {
		Account account = new Account(1000);

		for (int time = 1; time <= 200; time++) {
			account.updateLastKnown(prices(50, time));
		}

		assertEquals(200, account.getAccountHistory().getSessions().times().size());
		assertEquals(new Time(200), account.getAccountHistory().getSessions().times().getLast());
		assertEquals(200, account.getAccountHistory().getSessions().returns().length);
	}

	@Test
	void shouldCompoundYearToDateFromEndOfPreviousYear() throws AccountActionException {
		Account account = accountWithPosition();
		account.updateLastKnown(prices(55, 20251230)); // 1050: +5% last year
		account.updateLastKnown(prices(60, 20260102)); // 1100
		account.updateLastKnown(prices(66, 20260105)); // 1160

		assertEquals((1160.0 / 1050 - 1) * 100, account.getAccountHistory().getSessions().yearToDateReturnPercent(),
				1e-9);
	}

	@Test
	void shouldHaveYearToDateFromStartWithoutPreviousYear() throws AccountActionException {
		Account account = accountWithPosition();
		account.updateLastKnown(prices(60, 20260102)); // 1100

		assertEquals(10, account.getAccountHistory().getSessions().yearToDateReturnPercent(), 1e-9);
	}

	@Test
	void shouldHaveNoYearToDateWithoutSessions() throws AccountActionException {
		assertTrue(Double.isNaN(accountWithPosition().getAccountHistory().getSessions().yearToDateReturnPercent()));
	}

	private static TimeMarketDataSet prices(float price, int time) {
		FloatArraySlice prices = new FloatArraySlice(new float[]{price}, 0);
		FloatArraySlice volume = new FloatArraySlice(new float[]{1}, 0);
		TimeMarketData data = new TimeMarketData(MARKET, MarketType.STOCK, new Time(time),
				List.of(prices, prices, prices, prices, volume, volume));
		return new TimeMarketDataSet(Map.of(MARKET, data), new Time(time));
	}
}
