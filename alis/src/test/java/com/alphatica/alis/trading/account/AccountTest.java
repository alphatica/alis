package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.FloatArraySlice;
import com.alphatica.alis.data.market.Delisting;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.Deposit;
import com.alphatica.alis.trading.account.actions.Trade;
import com.alphatica.alis.trading.order.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

	@Test
	void shouldSellWholePositionOnDelistingAtZeroPrice() throws AccountActionException {
		MarketName market = new MarketName("a");
		MarketName other = new MarketName("b");
		Account account = new Account(1000);
		account.addPosition(market, new PositionEntry(new Time(1), 10, 50), 0);

		List<Trade> trades = account.processDelistings(List.of(new Delisting(market, new Time(2), 0.0),
				new Delisting(other, new Time(2), 1.0)), new Time(2));

		assertEquals(List.of(new Trade(market, Direction.SELL, 0.0, 10, 0.0)), trades);
		assertNull(account.getPosition(market));
		assertEquals(500, account.getCash(), 0.001);
		assertEquals(500, account.getNAV(), 0.001);
	}

	@Test
	void shouldApplySimpleActions() throws AccountActionException {
		Account account = new Account(0);
		new Deposit(5000).doOnAccount(new Time(1), account);
		assertEquals(5000, account.getNAV());
		assertEquals(5000, account.getCash());
		new Trade(new MarketName("market"), Direction.BUY, 100, 10, 1).doOnAccount(new Time(2), account);
		assertEquals(4999, account.getNAV());
		assertEquals(3999, account.getCash());
		Position actual = account.getPositions().get(new MarketName("market"));
		Position expected = new Position(new MarketName("market"), new PositionEntry(new Time(2), 10, 100));
		assertEquals(expected.getEntryPrice(), actual.getEntryPrice());
	}

	@Test
	void shouldCalculatePositionValueUsingLastClose() throws AccountActionException {
		MarketName market = new MarketName("market");
		Account account = new Account(2000);
		account.addPosition(market, new PositionEntry(new Time(1), 10, 100), 0);

		account.getPosition(market).updatePrices(125, 130, 95);

		assertEquals(1250, account.getPositionValue(market));
	}

	@Test
	void shouldIncludeCommissionsInClosedPositionStats() throws AccountActionException {
		MarketName market = new MarketName("market");
		Account account = new Account(2000);
		account.addPosition(market, new PositionEntry(new Time(1), 100, 10), 10);
		account.reducePosition(market, new PositionExit(new Time(2), 100, 10.1), 10);

		TradeStats stats = account.getStats();

		assertEquals(-10, account.calcCashProfit(), 0.000_001);
		assertEquals(-100.0 / 101.0, stats.profitPerTrade(), 0.000_001);
		assertEquals(-1, stats.expectancy(), 0.000_001);
	}

	@Test
	void downsideDrawdownIsZeroWhenOpenWinnerCoversClosedLosers() throws AccountActionException {
		MarketName winner = new MarketName("winner");
		MarketName loser = new MarketName("loser");
		Account account = new Account(100);
		account.addPosition(winner, new PositionEntry(new Time(1), 1, 50), 0);
		account.addPosition(loser, new PositionEntry(new Time(1), 1, 50), 0);
		setClose(account, winner, 90);
		setClose(account, loser, 20);

		account.reducePosition(loser, new PositionExit(new Time(2), 1, 20), 0);
		account.afterSells();
		account.reducePosition(winner, new PositionExit(new Time(3), 1, 90), 0);
		account.afterSells();

		assertEquals(0, account.getMaxDownsideDD(), 0.001);
	}

	@Test
	void downsideDrawdownIsZeroWhenPositionGivesBackItsOwnProfit() throws AccountActionException {
		MarketName market = new MarketName("market");
		Account account = new Account(100);
		account.addPosition(market, new PositionEntry(new Time(1), 1, 100), 0);
		setClose(account, market, 200);
		setClose(account, market, 100);

		assertEquals(0, account.getMaxDownsideDD(), 0.001);
	}

	@Test
	void closedProfitDoesNotCoverLaterLossesRegardlessOfOrderOfBuys() throws AccountActionException {
		MarketName a = new MarketName("a");
		MarketName b = new MarketName("b");

		Account boughtBefore = new Account(200);
		boughtBefore.addPosition(a, new PositionEntry(new Time(1), 1, 100), 0);
		boughtBefore.addPosition(b, new PositionEntry(new Time(1), 1, 100), 0);
		setClose(boughtBefore, a, 200);
		boughtBefore.reducePosition(a, new PositionExit(new Time(2), 1, 200), 0);
		boughtBefore.afterSells();
		setClose(boughtBefore, b, 50);

		Account boughtAfter = new Account(200);
		boughtAfter.addPosition(a, new PositionEntry(new Time(1), 1, 100), 0);
		setClose(boughtAfter, a, 200);
		boughtAfter.reducePosition(a, new PositionExit(new Time(2), 1, 200), 0);
		boughtAfter.afterSells();
		boughtAfter.addPosition(b, new PositionEntry(new Time(2), 1, 100), 0);
		setClose(boughtAfter, b, 50);

		assertEquals(-16.667, boughtBefore.getMaxDownsideDD(), 0.001);
		assertEquals(-16.667, boughtAfter.getMaxDownsideDD(), 0.001);
	}

	@Test
	void closedLossesAccumulateInDownsideDrawdown() throws AccountActionException {
		MarketName market = new MarketName("market");
		Account account = new Account(100);
		for (int i = 0; i < 3; i++) {
			int quantity = (int) Math.round(account.getCash() * 10);
			account.addPosition(market, new PositionEntry(new Time(i), quantity, 0.1), 0);
			setClose(account, market, 0.09);
			account.reducePosition(market, new PositionExit(new Time(i), quantity, 0.09), 0);
			account.afterSells();
		}

		assertEquals(-27.1, account.getMaxDownsideDD(), 0.001);
		assertEquals(-27.1, account.getCurrentDownsideDD(), 0.001);
	}

	@Test
	void partialSellOfWinnerRaisesDownsideReference() throws AccountActionException {
		MarketName market = new MarketName("market");
		Account account = new Account(100);
		account.addPosition(market, new PositionEntry(new Time(1), 10, 10), 0);
		setClose(account, market, 20);
		account.reducePosition(market, new PositionExit(new Time(2), 5, 20), 0);
		account.afterSells();
		assertEquals(0, account.getMaxDownsideDD(), 0.001);

		setClose(account, market, 10); // remaining part back at entry: not a downside drawdown
		assertEquals(0, account.getCurrentDownsideDD(), 0.001);

		setClose(account, market, 5); // below entry: loss of 25 from closed equity of 150
		assertEquals(-16.667, account.getCurrentDownsideDD(), 0.001);
	}

	@Test
	void withdrawalIsNotDownsideDrawdown() throws AccountActionException {
		Account account = new Account(100);
		account.reduceCash(50);

		assertEquals(0, account.getMaxDownsideDD(), 0.001);
	}

	@Test
	void depositDuringOpenLossKeepsLossAmount() throws AccountActionException {
		MarketName market = new MarketName("market");
		Account account = new Account(100);
		account.addPosition(market, new PositionEntry(new Time(1), 10, 10), 0);
		setClose(account, market, 8);

		account.addCash(100); // open loss 20 of 200
		assertEquals(-10, account.getCurrentDownsideDD(), 0.001);

		setClose(account, market, 10);
		assertEquals(0, account.getCurrentDownsideDD(), 0.001);
		assertEquals(-20, account.getMaxDownsideDD(), 0.001);
	}

	@Test
	void withdrawalDuringOpenLossKeepsLossAmount() throws AccountActionException {
		MarketName market = new MarketName("market");
		Account account = new Account(200);
		account.addPosition(market, new PositionEntry(new Time(1), 10, 10), 0);
		setClose(account, market, 5); // nav 150 of 200

		account.reduceCash(75); // open loss 50 of 125
		assertEquals(-40, account.getCurrentDownsideDD(), 0.001);

		setClose(account, market, 10);
		assertEquals(0, account.getCurrentDownsideDD(), 0.001);
	}

	@Test
	void cashFlowsKeepClosedLossPercentage() throws AccountActionException {
		MarketName market = new MarketName("market");
		Account account = new Account(100);
		account.addPosition(market, new PositionEntry(new Time(1), 10, 10), 0);
		account.reducePosition(market, new PositionExit(new Time(2), 10, 8), 0);
		account.afterSells();
		assertEquals(-20, account.getCurrentDownsideDD(), 0.001);

		account.addCash(80);
		assertEquals(-20, account.getCurrentDownsideDD(), 0.001);

		account.reduceCash(120);
		assertEquals(-20, account.getCurrentDownsideDD(), 0.001);
		assertEquals(-20, account.getMaxDownsideDD(), 0.001);
	}

	@Test
	void lossAfterDepositIsMeasuredAgainstDepositedCapital() throws AccountActionException {
		MarketName market = new MarketName("market");
		Account account = new Account(100);
		account.addCash(100);
		account.addPosition(market, new PositionEntry(new Time(1), 20, 10), 0);
		setClose(account, market, 9);

		assertEquals(-10, account.getMaxDownsideDD(), 0.001);
	}

	@Test
	void depositToEmptyAccountStartsDownsideDrawdownFromZero() throws AccountActionException {
		MarketName market = new MarketName("market");
		Account account = new Account(0);
		account.addCash(100);
		account.addPosition(market, new PositionEntry(new Time(1), 10, 10), 0);
		setClose(account, market, 9);

		assertEquals(-10, account.getMaxDownsideDD(), 0.001);

		account.reducePosition(market, new PositionExit(new Time(2), 10, 9), 0);
		account.reduceCash(90);
		account.afterSells();
		assertEquals(0, account.getCurrentDownsideDD(), 0.001);

		account.addCash(50);
		assertEquals(0, account.getCurrentDownsideDD(), 0.001);
		assertEquals(-10, account.getMaxDownsideDD(), 0.001);
	}

	@Test
	void updateLastKnownUpdatesBothDrawdowns() throws AccountActionException {
		MarketName market = new MarketName("market");
		Account account = new Account(100);
		account.addPosition(market, new PositionEntry(new Time(1), 10, 10), 0);
		account.updateLastKnown(marketDataSet(market, 10, 1));
		account.updateLastKnown(marketDataSet(market, 8, 2));

		assertEquals(80, account.getNAV(), 0.001);
		assertEquals(-20, account.getCurrentDD(), 0.001);
		assertEquals(-20, account.getCurrentDownsideDD(), 0.001);
	}

	@Test
	void updateLastKnownMeasuresFromClosedEquityRaisedBySell() throws AccountActionException {
		MarketName market = new MarketName("market");
		Account account = new Account(100);
		account.addPosition(market, new PositionEntry(new Time(1), 10, 10), 0);
		account.updateLastKnown(marketDataSet(market, 20, 1));
		account.reducePosition(market, new PositionExit(new Time(2), 5, 20), 0); // closed equity 100 + 5 * 10 = 150

		account.updateLastKnown(marketDataSet(market, 5, 3)); // nav 100 + 5 * 5 = 125

		assertEquals(-16.667, account.getCurrentDownsideDD(), 0.001);
	}

	@Test
	void closeIncludesExitCommissionInDownsideDrawdown() throws AccountActionException {
		MarketName market = new MarketName("market");
		Account account = new Account(100);
		account.addPosition(market, new PositionEntry(new Time(1), 10, 10), 0);
		setClose(account, market, 9); // nav 90

		account.close(0.1); // commission 9

		assertEquals(81, account.getCash(), 0.001);
		assertEquals(-19, account.getCurrentDownsideDD(), 0.001);
		assertEquals(-19, account.getMaxDownsideDD(), 0.001);
	}

	@Test
	void delistingWithProfitRaisesDownsideReference() throws AccountActionException {
		MarketName delisted = new MarketName("delisted");
		MarketName other = new MarketName("other");
		Account account = new Account(200);
		account.addPosition(delisted, new PositionEntry(new Time(1), 10, 10), 0);
		account.addPosition(other, new PositionEntry(new Time(1), 10, 10), 0);
		setClose(account, delisted, 20);
		setClose(account, other, 5); // nav 250, closed equity 200

		account.processDelistings(List.of(new Delisting(delisted, new Time(2), 20)), new Time(2)); // closed equity 300

		assertEquals(-16.667, account.getCurrentDownsideDD(), 0.001);
	}

	@Test
	void depositAfterLosingWholeCapitalStartsFromDeposit() throws AccountActionException {
		MarketName market = new MarketName("market");
		Account account = new Account(100);
		account.addPosition(market, new PositionEntry(new Time(1), 10, 10), 0);

		account.processDelistings(List.of(new Delisting(market, new Time(2), 0)), new Time(2));
		assertEquals(-100, account.getCurrentDownsideDD(), 0.001);

		account.addCash(50);
		assertEquals(0, account.getCurrentDownsideDD(), 0.001);
		assertEquals(-100, account.getMaxDownsideDD(), 0.001);
	}

	@Test
	void rejectsNonPositiveDepositAndWithdrawal() {
		Account account = new Account(100);

		assertThrows(IllegalArgumentException.class, () -> account.addCash(0));
		assertThrows(IllegalArgumentException.class, () -> account.reduceCash(0));
		assertEquals(100, account.getCash(), 0.001);
	}

	@Test
	void rejectsWithdrawalAboveCashWithoutChangingAccount() throws AccountActionException {
		MarketName market = new MarketName("market");
		Account account = new Account(200);
		account.addPosition(market, new PositionEntry(new Time(1), 10, 10), 0);

		AccountActionException e = assertThrows(AccountActionException.class, () -> account.reduceCash(150));

		assertTrue(e.getMessage().contains("150") && e.getMessage().contains("100"), e.getMessage());
		assertEquals(100, account.getCash(), 0.001);
		assertEquals(200, account.getNAV(), 0.001);
		assertEquals(0, account.getMaxDownsideDD(), 0.001);
		account.reduceCash(100); // exactly available cash is allowed
		assertEquals(0, account.getCash(), 0.001);
	}

	@Test
	void depositAndWithdrawalAreNotDrawdown() throws AccountActionException {
		MarketName market = new MarketName("market");
		Account account = new Account(100);
		account.addPosition(market, new PositionEntry(new Time(1), 5, 10), 0);
		account.updateLastKnown(marketDataSet(market, 10, 1));
		account.updateLastKnown(marketDataSet(market, 8, 2)); // nav 90

		account.addCash(100);
		account.updateLastKnown(marketDataSet(market, 8, 3));
		assertEquals(-10, account.getCurrentDD(), 0.001);

		account.reduceCash(120);
		account.updateLastKnown(marketDataSet(market, 8, 4));
		assertEquals(-10, account.getCurrentDD(), 0.001);
	}

	@Test
	void delistingUpdatesDrawdown() throws AccountActionException {
		MarketName market = new MarketName("market");
		Account account = new Account(100);
		account.addPosition(market, new PositionEntry(new Time(1), 5, 10), 0);
		account.updateLastKnown(marketDataSet(market, 10, 1));

		account.processDelistings(List.of(new Delisting(market, new Time(2), 2)), new Time(2)); // nav 60

		assertEquals(-40, account.getCurrentDD(), 0.001);
		assertEquals(-40, account.getCurrentDownsideDD(), 0.001);
	}

	private static TimeMarketDataSet marketDataSet(MarketName market, float price, int time) {
		FloatArraySlice prices = new FloatArraySlice(new float[]{price}, 0);
		FloatArraySlice volume = new FloatArraySlice(new float[]{1}, 0);
		TimeMarketData data = new TimeMarketData(market, MarketType.STOCK, new Time(time),
				List.of(prices, prices, prices, prices, volume, volume));
		return new TimeMarketDataSet(Map.of(market, data), new Time(time));
	}

	private static void setClose(Account account, MarketName market, double close) {
		account.getPosition(market).updatePrices(close, close, close);
		account.afterSells();
	}

}
