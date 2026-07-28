package com.alphatica.alis.trading.strategy;

import com.alphatica.alis.data.FloatArraySlice;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
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
import static com.alphatica.alis.trading.order.OrderSize.COUNT;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

	private static StrategyTradeExecutor tradeExecutor(Double limitOrderSize) {
		return new StrategyTradeExecutor(0.0, TradePrice.OPEN, limitOrderSize,
				new StrategyExecutionLogger(false));
	}

	private static TimeMarketDataSet marketDataSet(float price, float turnover) {
		FloatArraySlice priceData = new FloatArraySlice(new float[]{price}, 0);
		FloatArraySlice turnoverData = new FloatArraySlice(new float[]{turnover}, 0);
		TimeMarketData marketData = new TimeMarketData(MARKET, MarketType.STOCK, TIME,
				List.of(priceData, priceData, priceData, priceData, turnoverData));
		return new TimeMarketDataSet(Map.of(MARKET, marketData), TIME);
	}
}
