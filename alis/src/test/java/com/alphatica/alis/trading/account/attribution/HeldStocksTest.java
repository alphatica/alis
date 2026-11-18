package com.alphatica.alis.trading.account.attribution;

import com.alphatica.alis.data.StandardMarketData;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.tools.data.TestMarket;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.PositionEntry;
import com.alphatica.alis.trading.account.PositionExit;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class HeldStocksTest {
	private static final double DELTA = 1e-9;
	private static final int[] TIMES = {20261102, 20261103, 20261104, 20261105, 20261106};
	private static final MarketName A = new MarketName("a");
	private static final MarketName B = new MarketName("b");
	private static final MarketName BENCHMARK = new MarketName("benchmark");

	@Test
	void shouldBeFlatForEqualSizesWhenPricesDrift() throws AccountActionException {
		StandardMarketData data = data(new float[]{10, 10, 20, 10, 10}, new float[]{10, 10, 10, 10, 10});
		Account account = new Account(1000);
		session(account, data, 0);
		buy(account, A, 1, 10, 10);
		buy(account, B, 1, 10, 10);
		for (int session = 1; session < TIMES.length; session++) {
			session(account, data, session);
		}

		assertArrayEquals(new double[]{0, 0, 0, 0, 0}, sizeEffect(account, data), DELTA);
	}

	@Test
	void shouldNotRebalanceEqualPositionsWhenPricesGoBack() throws AccountActionException {
		// Rebalanced every session to equal weights, the stocks would gain 100 * ln(1 + (-1/6 + 1/4) / 2)
		StandardMarketData data = data(new float[]{10, 10, 12, 10, 10}, new float[]{10, 10, 8, 10, 10});
		Account account = new Account(1000);
		session(account, data, 0);
		buy(account, A, 1, 10, 10);
		buy(account, B, 1, 10, 10);
		for (int session = 1; session < TIMES.length; session++) {
			session(account, data, session);
		}

		assertArrayEquals(new double[]{0, 0, 0, 0, 0}, versusBenchmark(account, data), DELTA);
	}

	@Test
	void shouldWeightBySizeAtPurchaseChangingWithPrice() throws AccountActionException {
		StandardMarketData data = data(new float[]{10, 10, 20, 10, 10}, new float[]{10, 10, 10, 10, 10});
		Account account = new Account(1000);
		session(account, data, 0);
		buy(account, A, 1, 20, 10);
		buy(account, B, 1, 10, 10);
		for (int session = 1; session < TIMES.length; session++) {
			session(account, data, session);
		}

		// A doubles on 200 of 300 against 1 of 2 equal positions, then halves on 400 of 500 against 2 of 3: back at the
		// prices of the purchase, the sizes made nothing
		double first = 100 * (Math.log(5.0 / 3) - Math.log(1.5));
		double second = first + 100 * (Math.log(0.6) - Math.log(2.0 / 3));
		assertArrayEquals(new double[]{0, 0, first, second, second}, sizeEffect(account, data), DELTA);
		assertEquals(0, second, DELTA);
	}

	@Test
	void shouldTakeNavAtStartOfBuyingSession() throws AccountActionException {
		StandardMarketData data = data(new float[]{10, 10, 20, 40, 20}, new float[]{10, 10, 10, 10, 15});
		Account account = new Account(1000);
		session(account, data, 0);
		buy(account, A, 1, 10, 10);
		session(account, data, 1);
		session(account, data, 2);
		buy(account, B, 3, 11, 10);
		session(account, data, 3);
		session(account, data, 4);

		assertArrayEquals(new double[]{0, 0, 0, 0, 0}, sizeEffect(account, data), DELTA);
	}

	@Test
	void shouldTakeAwaySoldPart() throws AccountActionException {
		StandardMarketData data = data(new float[]{10, 10, 20, 10, 10}, new float[]{10, 10, 10, 10, 10});
		Account account = new Account(1000);
		session(account, data, 0);
		buy(account, A, 1, 20, 10);
		buy(account, B, 1, 10, 10);
		session(account, data, 1);
		account.reducePosition(A, new PositionExit(new Time(TIMES[2]), 10, 20), 0);
		for (int session = 2; session < TIMES.length; session++) {
			session(account, data, session);
		}

		double first = 100 * (Math.log(5.0 / 3) - Math.log(1.5));
		assertArrayEquals(new double[]{0, 0, first, first, first}, sizeEffect(account, data), DELTA);
	}

	@Test
	void shouldAddPurchasesOfSameStock() throws AccountActionException {
		StandardMarketData data = data(new float[]{10, 10, 10, 20, 20}, new float[]{10, 10, 10, 10, 10});
		Account account = new Account(1000);
		session(account, data, 0);
		buy(account, A, 1, 10, 10);
		buy(account, B, 1, 10, 10);
		session(account, data, 1);
		buy(account, A, 2, 10, 10);
		for (int session = 2; session < TIMES.length; session++) {
			session(account, data, session);
		}

		double added = 100 * (Math.log(5.0 / 3) - Math.log(1.5));
		assertArrayEquals(new double[]{0, 0, 0, added, added}, sizeEffect(account, data), DELTA);
	}

	private static double[] sizeEffect(Account account, StandardMarketData data) {
		return HeldStocks.of(account, data, data.getMarket(BENCHMARK)).sizeEffect();
	}

	private static double[] versusBenchmark(Account account, StandardMarketData data) {
		return HeldStocks.of(account, data, data.getMarket(BENCHMARK)).versusBenchmark();
	}

	private static StandardMarketData data(float[] a, float[] b) {
		StandardMarketData data = new StandardMarketData();
		Market benchmark = TestMarket.withCloses(BENCHMARK, TIMES, new float[]{100, 100, 100, 100, 100});
		data.addMarkets(Map.of(A, TestMarket.withCloses(A, TIMES, a), B, TestMarket.withCloses(B, TIMES, b),
				BENCHMARK, benchmark));
		return data;
	}

	private static void buy(Account account, MarketName market, int session, double quantity, double price)
			throws AccountActionException {
		account.addPosition(market, new PositionEntry(new Time(TIMES[session]), quantity, price), 0);
	}

	private static void session(Account account, StandardMarketData data, int session) {
		account.updateLastKnown(data.snapshotAt(new Time(TIMES[session])));
	}
}
