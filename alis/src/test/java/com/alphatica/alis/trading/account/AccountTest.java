package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.Deposit;
import com.alphatica.alis.trading.account.actions.Trade;
import com.alphatica.alis.trading.order.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

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

		TradeStats stats = account.getAccountHistory().getStats();

		assertEquals(-10, account.calcCashProfit(), 0.000_001);
		assertEquals(-100.0 / 101.0, stats.profitPerTrade(), 0.000_001);
		assertEquals(-1, stats.expectancy(), 0.000_001);
	}

}
