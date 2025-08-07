package com.alphatica.alis.trading.strategy;

import com.alphatica.alis.tools.data.TestData;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StrategyExecutorTests {

	@Test
	void shouldExecuteStrategy() throws AccountActionException {
		/*
			1. Start with 100k
			2. Buy for 50k and pay 5k commission
			3. Sell for 100k and pay 10k commission
			4. Nav = 100 -50 -5 +100 -10 = 135k
		 */
		double commissionRate = 0.1;
		double initialCash = 100_000;
		StrategyExecutor executor = new StrategyExecutor().withCommissionRate(commissionRate).withInitialCash(initialCash);
		Account account = executor.execute(new TestData("test_market"), new TestStrategy());
		assertEquals(0, account.getPositions().size());
		double nav = 135_000;
		assertEquals(nav, account.getNAV());
	}

	@Test
	void shouldRemoveTrades() throws AccountActionException {
		/*
			1. Start with 100k
			2. Send orders but have them removed
			3. Nav must be the same.
		 */
		StrategyExecutor executor = new StrategyExecutor();
		executor.skipTrades(1.00);
		Account account = executor.execute(new TestData("test_market"), new TestStrategy());
		assertEquals(0, account.getPositions().size());
		double nav = 100_000;
		assertEquals(nav, account.getNAV());
	}
}
