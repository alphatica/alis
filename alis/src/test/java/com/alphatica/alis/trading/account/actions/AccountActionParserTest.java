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
}