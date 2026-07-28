package com.alphatica.alis.trading.datamining.betterexits;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.tools.data.TestData;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.PositionEntry;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExitIfSmallProfitAfterTest {

	private static final MarketName MARKET = new MarketName("market");

	@Test
	void shouldCheckActualPositionProfitAfterConfiguredNumberOfBars()
			throws AccountActionException {
		TestData marketData = new TestData(MARKET.toString());
		Account account = new Account(100);
		account.addPosition(MARKET, new PositionEntry(new Time(2), 1, 10), 0);
		ExitIfSmallProfitAfter finder = new ExitIfSmallProfitAfter(-50, 2);
		MarketStateSet marketStateSet = new MarketStateSet();

		assertFalse(finder.shouldExit(
				account,
				marketData.snapshotAt(new Time(2)).get(MARKET),
				marketData.snapshotAt(new Time(2)),
				marketStateSet));
		assertTrue(finder.shouldExit(
				account,
				marketData.snapshotAt(new Time(3)).get(MARKET),
				marketData.snapshotAt(new Time(3)),
				marketStateSet));
	}
}
