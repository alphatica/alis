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

}