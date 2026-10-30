package com.alphatica.alis.trading.account.scorer;

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

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReturnOnInvestedCapitalTest {
	private static final double DELTA = 1e-9;
	private static final MarketName MARKET = new MarketName("market");
	private static final Time MON_5 = new Time(20260105);
	private static final Time THU_8 = new Time(20260108);
	private static final Time MON_12 = new Time(20260112);
	private static final Time THU_15 = new Time(20260115);
	private static final Time MON_19 = new Time(20260119);
	private static final Time MON_26 = new Time(20260126);

	private final ReturnOnInvestedCapital scorer = new ReturnOnInvestedCapital();

	@Test
	void shouldPoolProfitOverCapitalDaysOfEveryLot() throws AccountActionException {
		Account account = new Account(10_000);
		account.addPosition(MARKET, new PositionEntry(MON_5, 10, 50), 2);
		account.updateLastKnown(prices(50, MON_5));
		account.addPosition(MARKET, new PositionEntry(THU_8, 5, 60), 1);
		account.updateLastKnown(prices(60, THU_8));
		account.reducePosition(MARKET, new PositionExit(MON_12, 10, 58), 3);
		account.updateLastKnown(prices(58, MON_12));
		account.reducePosition(MARKET, new PositionExit(MON_19, 5, 62), 1);
		account.updateLastKnown(prices(62, MON_19));
		account.updateLastKnown(prices(62, MON_26));

		// Lots sold first in, first out: 10 x 50 held 7 days, 5 x 60 held 11 days (not until the last session);
		// profit 10 * 8 + 5 * 2 minus exit commissions 3 + 1, entry commissions left out
		assertEquals(365.0 * 86 / (10 * 50 * 7 + 5 * 60 * 11) * 100, scorer.score(account, Map.of()), DELTA);
	}

	@Test
	void shouldValueOpenPositionAtLastCloseUntilLastSession() throws AccountActionException {
		// 4 x 25 held 10 days, worth 4 x 30 at the last close
		assertEquals(365.0 * 20 / (100 * 10) * 100, scorer.score(accountHoldingOpenPosition(10_000), Map.of()), DELTA);
	}

	@Test
	void shouldChargeExitCommissionOfPositionsClosedWithAccount() throws AccountActionException {
		Account account = accountHoldingOpenPosition(10_000);

		account.close(0.01);

		// Sold at the last close 30, exit commission 4 x 30 x 0.01
		assertEquals(365.0 * (20 - 1.2) / (100 * 10) * 100, scorer.score(account, Map.of()), DELTA);
	}

	@Test
	void shouldNotDependOnCashOfAccount() throws AccountActionException {
		assertEquals(scorer.score(accountHoldingOpenPosition(10_000), Map.of()),
				scorer.score(accountHoldingOpenPosition(1_000_000), Map.of()), DELTA);
	}

	@Test
	void shouldCountLotSoldInSessionOfPurchaseAsHeldOneDay() throws AccountActionException {
		Account account = new Account(10_000);
		account.addPosition(MARKET, new PositionEntry(MON_5, 10, 50), 0.5);
		account.reducePosition(MARKET, new PositionExit(MON_5, 10, 53), 0.5);
		account.updateLastKnown(prices(53, MON_5));

		assertEquals(365.0 * (30 - 0.5) / (10 * 50) * 100, scorer.score(account, Map.of()), DELTA);
	}

	@Test
	void shouldScoreZeroWithoutTrades() {
		Account withSessions = new Account(10_000);
		withSessions.updateLastKnown(prices(50, MON_5));
		withSessions.updateLastKnown(prices(55, MON_12));

		assertEquals(0, scorer.score(new Account(10_000), Map.of()));
		assertEquals(0, scorer.score(withSessions, Map.of()));
	}

	private static Account accountHoldingOpenPosition(double cash) throws AccountActionException {
		Account account = new Account(cash);
		account.addPosition(MARKET, new PositionEntry(MON_5, 4, 25), 1);
		account.updateLastKnown(prices(25, MON_5));
		account.updateLastKnown(prices(30, THU_15));
		return account;
	}

	private static TimeMarketDataSet prices(float price, Time time) {
		FloatArraySlice prices = new FloatArraySlice(new float[]{price}, 0);
		FloatArraySlice volume = new FloatArraySlice(new float[]{1}, 0);
		TimeMarketData data = new TimeMarketData(MARKET, MarketType.STOCK, time,
				List.of(prices, prices, prices, prices, volume, volume));
		return new TimeMarketDataSet(Map.of(MARKET, data), time);
	}
}
