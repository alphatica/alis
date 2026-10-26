package com.alphatica.alis.trading.account.actions;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.order.Direction;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AccountActionParserTest {

	@Test
	void shouldParseTradeBuy() {
		AccountAction action = AccountActionParser.fromCsv("20250211,Trade,gig,Buy,1.98,4025,30.28");
		assertEquals(new AccountAction(new Time(20250211), new Trade(new MarketName("gig"), Direction.BUY, 1.98, 4025, 30.28)), action);
	}

	@Test
	void shouldParseTradeSell() {
		AccountAction action = AccountActionParser.fromCsv("20250211,Trade,gig,Sell,1.98,4025,30.28");
		assertEquals(new AccountAction(new Time(20250211), new Trade(new MarketName("gig"), Direction.SELL, 1.98, 4025, 30.28)), action);
	}

	@Test
	void shouldParseDeposit() {
		AccountAction action = AccountActionParser.fromCsv("20250317,Deposit,5000.00");
		assertEquals(new AccountAction(new Time(20250317), new Deposit(5000.00)), action);
	}

	@Test
	void shouldParseWithdrawal() {
		AccountAction action = AccountActionParser.fromCsv("20250317,Withdrawal,5000.00");
		assertEquals(new AccountAction(new Time(20250317), new Withdrawal(5000.00)), action);
	}

	@Test
	void shouldParseFractionalQuantity() {
		AccountAction action = AccountActionParser.fromCsv("20250211,Trade,gig,Buy,1.98,0.125,0.5");
		assertEquals(new AccountAction(new Time(20250211), new Trade(new MarketName("gig"), Direction.BUY, 1.98, 0.125, 0.5)), action);
	}

	@Test
	void shouldParseSmallestQuantity() {
		AccountAction action = AccountActionParser.fromCsv("20250211,Trade,gig,Buy,1.98,0.000001,0");
		assertEquals(0.000001, ((Trade) action.actionType()).quantity());
	}

	@Test
	void shouldRejectQuantityWithTooManyDecimalPlaces() {
		var exception = assertThrows(IllegalArgumentException.class,
				() -> AccountActionParser.fromCsv("20250211,Trade,gig,Buy,1.98,0.1234567,0"));
		assertTrue(exception.getMessage().contains("0.1234567"), exception.getMessage());
	}

	@Test
	void shouldRejectZeroQuantity() {
		assertThrows(IllegalArgumentException.class, () -> AccountActionParser.fromCsv("20250211,Trade,gig,Buy,1.98,0,0"));
	}

	@Test
	void shouldWriteWholeQuantityWithoutFraction() {
		Trade trade = new Trade(new MarketName("gig"), Direction.BUY, 1.98, 4025, 30.28);
		assertEquals("Trade,gig,Buy,1.98,4025,30.28", trade.toCsv());
	}

	@Test
	void shouldWriteAndReadFractionalQuantity() {
		Trade trade = new Trade(new MarketName("gig"), Direction.SELL, 1.98, 0.1 + 0.2, 0.5);
		assertEquals("Trade,gig,Sell,1.98,0.3,0.50", trade.toCsv());
		AccountAction parsed = AccountActionParser.fromCsv("20250211," + trade.toCsv());
		assertEquals(trade, parsed.actionType());
	}

	@Test
	void shouldRoundTradeQuantityToSmallestUnit() {
		Trade trade = new Trade(new MarketName("gig"), Direction.BUY, 1.98, 1.2345675001, 0);
		assertEquals(1.234568, trade.quantity());
	}
}
