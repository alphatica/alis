package com.alphatica.alis.trading.strategy;

import com.alphatica.alis.data.FloatArraySlice;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.PositionEntry;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.Trade;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.order.TradePrice;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;
import static com.alphatica.alis.trading.order.OrderSize.COUNT;
import static com.alphatica.alis.trading.order.OrderSize.PERCENTAGE;
import static com.alphatica.alis.trading.order.OrderSize.VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrategyTradeExecutorTest {

	private static final Time TIME = new Time(10);
	private static final MarketName MARKET = new MarketName("market");

	@Test
	void shouldStopBuyingWhenHighestPriorityOrderCannotBeFunded() throws AccountActionException {
		Account account = new Account(100.0);
		var orders = new ArrayList<>(List.of(
				new Order(MARKET, BUY, COUNT, 20, 2.0),
				new Order(MARKET, BUY, COUNT, 1, 1.0)));

		tradeExecutor(Double.NaN).executeBuys(orders, marketDataSet(10.0f, 1_000.0f), account);

		assertEquals(2, orders.size());
		assertTrue(account.getPositions().isEmpty());
	}

	@Test
	void shouldKeepUnavailableAndZeroQuantityOrdersPending() throws AccountActionException {
		Account account = new Account(100.0);
		var orders = new ArrayList<>(List.of(
				new Order(new MarketName("missing"), BUY, COUNT, 1, 2.0),
				new Order(MARKET, BUY, COUNT, 0, 1.0)));

		tradeExecutor(Double.NaN).executeBuys(orders, marketDataSet(10.0f, 1_000.0f), account);

		assertEquals(2, orders.size());
		assertTrue(account.getPositions().isEmpty());
	}

	@Test
	void shouldLimitBuyQuantityToMarketLiquidity() throws AccountActionException {
		Account account = new Account(1_000.0);
		var orders = new ArrayList<>(List.of(new Order(MARKET, BUY, COUNT, 10, 1.0)));

		tradeExecutor(0.2).executeBuys(orders, marketDataSet(10.0f, 100.0f), account);

		assertTrue(orders.isEmpty());
		assertEquals(2, account.getPosition(MARKET).getQuantity());
		Trade trade = account.getAccountHistory().getActions().stream()
				.map(AccountAction::actionType)
				.filter(Trade.class::isInstance)
				.map(Trade.class::cast)
				.findFirst()
				.orElseThrow();
		assertEquals(2, trade.quantity());
	}

	@Test
	void shouldBuyForValueIncludingCommission() throws AccountActionException {
		Account account = new Account(1_000.0);
		var orders = new ArrayList<>(List.of(new Order(MARKET, BUY, VALUE, 55, 1.0)));

		tradeExecutor(Double.NaN, 0.25).executeBuys(orders, marketDataSet(10.0f, 1_000.0f), account);

		assertTrue(orders.isEmpty());
		assertEquals(4.4, account.getPosition(MARKET).getQuantity());
		assertEquals(945, account.getCash(), 1e-9);
		assertEquals(4.4, lastTrade(account).quantity());
		assertEquals(11, lastTrade(account).commission(), 1e-9);
	}

	@Test
	void shouldRoundValueBuyDownToSmallestQuantity() throws AccountActionException {
		Account account = new Account(1_000.0);
		var orders = new ArrayList<>(List.of(new Order(MARKET, BUY, VALUE, 100, 1.0)));

		tradeExecutor(Double.NaN, 0.0).executeBuys(orders, marketDataSet(3.0f, 1_000.0f), account);

		assertEquals(33.333333, account.getPosition(MARKET).getQuantity());
		assertEquals(900.000001, account.getCash(), 1e-9);
	}

	@Test
	void shouldIgnoreValueBuyBelowSmallestQuantity() throws AccountActionException {
		Account account = new Account(1_000.0);
		var orders = new ArrayList<>(List.of(new Order(MARKET, BUY, VALUE, 0.000009, 1.0)));

		tradeExecutor(Double.NaN, 0.0).executeBuys(orders, marketDataSet(10.0f, 1_000.0f), account);

		assertEquals(1, orders.size());
		assertTrue(account.getPositions().isEmpty());
		assertEquals(1_000, account.getCash());
	}

	@Test
	void shouldSellPartOfPositionForValue() throws AccountActionException {
		Account account = accountWithPosition(3);
		var orders = new ArrayList<>(List.of(new Order(MARKET, SELL, VALUE, 17, 1.0)));

		tradeExecutor(Double.NaN, 0.0).executeSells(orders, marketDataSet(10.0f, 1_000.0f), account);

		assertTrue(orders.isEmpty());
		assertEquals(1.3, account.getPosition(MARKET).getQuantity());
		assertEquals(1.7, lastTrade(account).quantity());
	}

	@Test
	void shouldSellAtMostWholePositionForValue() throws AccountActionException {
		Account account = accountWithPosition(3);
		var orders = new ArrayList<>(List.of(new Order(MARKET, SELL, VALUE, 1_000, 1.0)));

		tradeExecutor(Double.NaN, 0.0).executeSells(orders, marketDataSet(10.0f, 1_000.0f), account);

		assertNull(account.getPosition(MARKET));
		assertEquals(3, lastTrade(account).quantity());
	}

	@Test
	void shouldKeepSellOrderBelowSmallestQuantityPending() throws AccountActionException {
		Account account = accountWithPosition(3);
		var orders = new ArrayList<>(List.of(new Order(MARKET, SELL, VALUE, 0.000005, 1.0)));

		tradeExecutor(Double.NaN, 0.0).executeSells(orders, marketDataSet(10.0f, 1_000.0f), account);

		assertEquals(1, orders.size());
		assertEquals(3, account.getPosition(MARKET).getQuantity());
	}

	@Test
	void shouldBuyFractionalPercentageOfNav() throws AccountActionException {
		Account account = new Account(1_000.0);
		var orders = new ArrayList<>(List.of(new Order(MARKET, BUY, PERCENTAGE, 15, 1.0)));

		tradeExecutor(Double.NaN, 0.0).executeBuys(orders, marketDataSet(12.0f, 1_000.0f), account);

		assertEquals(12.5, account.getPosition(MARKET).getQuantity());
	}

	@Test
	void shouldSellFractionalPercentageOfPosition() throws AccountActionException {
		Account account = accountWithPosition(3);
		var orders = new ArrayList<>(List.of(new Order(MARKET, SELL, PERCENTAGE, 50, 1.0)));

		tradeExecutor(Double.NaN, 0.0).executeSells(orders, marketDataSet(10.0f, 1_000.0f), account);

		assertEquals(1.5, account.getPosition(MARKET).getQuantity());
	}

	@Test
	void shouldBuyFractionalCount() throws AccountActionException {
		Account account = new Account(1_000.0);
		var orders = new ArrayList<>(List.of(new Order(MARKET, BUY, COUNT, 2.7, 1.0)));

		tradeExecutor(Double.NaN, 0.0).executeBuys(orders, marketDataSet(10.0f, 1_000.0f), account);

		assertEquals(2.7, account.getPosition(MARKET).getQuantity());
	}

	@Test
	void shouldRoundLiquidityLimitDownToSmallestQuantity() throws AccountActionException {
		Account account = new Account(1_000.0);
		var orders = new ArrayList<>(List.of(new Order(MARKET, BUY, COUNT, 10, 1.0)));

		tradeExecutor(1.0, 0.0).executeBuys(orders, marketDataSet(3.0f, 10.0f), account);

		assertEquals(3.333333, account.getPosition(MARKET).getQuantity());
	}

	@Test
	void shouldNotBuyWhenCommissionMakesOrderTooExpensive() throws AccountActionException {
		Account account = new Account(100.0);
		var orders = new ArrayList<>(List.of(new Order(MARKET, BUY, COUNT, 10, 1.0)));

		tradeExecutor(Double.NaN, 0.01).executeBuys(orders, marketDataSet(10.0f, 1_000.0f), account);

		assertEquals(1, orders.size());
		assertTrue(account.getPositions().isEmpty());
	}

	@Test
	void shouldBuyWhenCostEqualsCash() throws AccountActionException {
		Account account = new Account(101.0);
		var orders = new ArrayList<>(List.of(new Order(MARKET, BUY, COUNT, 10, 1.0)));

		tradeExecutor(Double.NaN, 0.01).executeBuys(orders, marketDataSet(10.0f, 1_000.0f), account);

		assertTrue(orders.isEmpty());
		assertEquals(0, account.getCash(), 1e-9);
	}

	private static Account accountWithPosition(double quantity) throws AccountActionException {
		Account account = new Account(1_000.0);
		account.addPosition(MARKET, new PositionEntry(TIME, quantity, 10), 0);
		return account;
	}

	private static Trade lastTrade(Account account) {
		return account.getAccountHistory().getActions().stream()
				.map(AccountAction::actionType)
				.filter(Trade.class::isInstance)
				.map(Trade.class::cast)
				.reduce((first, second) -> second)
				.orElseThrow();
	}

	private static StrategyTradeExecutor tradeExecutor(Double limitOrderSize) {
		return tradeExecutor(limitOrderSize, 0.0);
	}

	private static StrategyTradeExecutor tradeExecutor(Double limitOrderSize, double commissionRate) {
		return new StrategyTradeExecutor(commissionRate, TradePrice.OPEN, limitOrderSize,
				new StrategyExecutionLogger(false));
	}

	private static TimeMarketDataSet marketDataSet(float price, float turnover) {
		FloatArraySlice priceData = new FloatArraySlice(new float[]{price}, 0);
		FloatArraySlice volumeData = new FloatArraySlice(new float[]{turnover / price}, 0);
		FloatArraySlice turnoverData = new FloatArraySlice(new float[]{turnover}, 0);
		TimeMarketData marketData = new TimeMarketData(MARKET, MarketType.STOCK, TIME,
				List.of(priceData, priceData, priceData, priceData, volumeData, turnoverData));
		return new TimeMarketDataSet(Map.of(MARKET, marketData), TIME);
	}
}
