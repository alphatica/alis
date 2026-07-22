package com.alphatica.alis.trading.account;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountHistoryTest {

	@Test
	void shouldReturnZeroStatsWhenThereAreNoTrades() {
		AccountHistory history = new AccountHistory(100_000);

		TradeStats stats = history.getStats();

		assertEquals(new TradeStats(0, 0, 0, 0, 0, 0, 0, 0), stats);
	}
}
