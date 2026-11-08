package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.data.FloatArraySlice;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.PositionEntry;
import com.alphatica.alis.trading.account.PositionExit;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ResultStatsTest {
	private static final double DELTA = 1e-9;
	private static final MarketName A = new MarketName("a");
	private static final MarketName B = new MarketName("b");
	private static final MarketName C = new MarketName("c");

	@Test
	void shouldGiveStatsOfAccount() throws AccountActionException {
		Account account = new Account(1000);
		account.addPosition(A, new PositionEntry(new Time(1), 10, 10), 0);
		account.addPosition(B, new PositionEntry(new Time(1), 10, 10), 0);
		account.addPosition(C, new PositionEntry(new Time(1), 10, 10), 0);
		account.updateLastKnown(closes(1, Map.of(A, 10f, B, 10f, C, 10f)));
		account.updateLastKnown(closes(2, Map.of(A, 12f, B, 10f, C, 5f)));
		account.reducePosition(A, new PositionExit(new Time(2), 10, 12), 0);
		account.reducePosition(C, new PositionExit(new Time(2), 10, 5), 0);
		account.afterSells();
		account.addPosition(A, new PositionEntry(new Time(3), 5, 12), 0);
		account.updateLastKnown(closes(3, Map.of(A, 15f, B, 11f)));
		account.reducePosition(A, new PositionExit(new Time(3), 5, 15), 0);
		account.reducePosition(B, new PositionExit(new Time(3), 10, 11), 0);
		account.afterSells();

		ResultStats stats = ResultStats.of(account);

		assertEquals(995, stats.nav(), DELTA);
		assertEquals(-3, stats.maxDD(), DELTA);
		assertEquals(-0.5 / 3, stats.returnToMaxDownsideDD(), DELTA);
		assertEquals(4, stats.trades());
		assertEquals(75, stats.accuracy(), DELTA);
		assertEquals(0.025, stats.expectancy(), DELTA);
		assertEquals(1.1, stats.profitFactor(), DELTA);
		assertEquals(1.25, stats.profitPerTrade(), DELTA);
		assertEquals(200.0 / 3, stats.profitableMarketsPercent(), DELTA);
	}

	private static TimeMarketDataSet closes(int time, Map<MarketName, Float> prices) {
		Map<MarketName, TimeMarketData> set = new HashMap<>();
		prices.forEach((market, price) -> {
			FloatArraySlice close = new FloatArraySlice(new float[]{price}, 0);
			FloatArraySlice volume = new FloatArraySlice(new float[]{1}, 0);
			set.put(market, new TimeMarketData(market, MarketType.STOCK, new Time(time),
					List.of(close, close, close, close, volume, volume)));
		});
		return new TimeMarketDataSet(set, new Time(time));
	}
}
