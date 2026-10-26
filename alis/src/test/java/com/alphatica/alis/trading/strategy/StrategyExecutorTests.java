package com.alphatica.alis.trading.strategy;

import com.alphatica.alis.data.market.Delisting;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.TestData;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.Trade;
import com.alphatica.alis.trading.order.Order;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.alphatica.alis.tools.java.CollectionsTools.arrayList;
import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;
import static com.alphatica.alis.trading.order.OrderSize.PERCENTAGE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrategyExecutorTests {

	@Test
	void shouldExecuteStrategy() throws AccountActionException {
		/*
			1. Start with 100k
			2. Buy 50k / (100 * 1.1) = 454.545454 shares (rounded down to 0.000001)
			   for 45454.5454 and pay 4545.45454 commission
			3. Sell at 200 for 90909.0908 and pay 9090.90908 commission
			4. Nav = 100000 - 45454.5454 - 4545.45454 + 90909.0908 - 9090.90908 = 131818.18178
		 */
		double commissionRate = 0.1;
		double initialCash = 100_000;
		StrategyExecutor executor = new StrategyExecutor().withCommissionRate(commissionRate).withInitialCash(initialCash);
		Account account = executor.execute(new TestData("test_market"), new TestStrategy());
		assertEquals(0, account.getPositions().size());
		double nav = 131_818.18178;
		assertEquals(nav, account.getNAV(), 1e-6);
	}

	@Test
	void shouldSellPositionAtDelistingPriceWithoutCommission() throws AccountActionException {
		/*
			1. Start with 100k
			2. Buy 454.545454 shares at 100 for 45454.5454 and pay 4545.45454 commission
			3. Delisting at 120 without commission: + 54545.45448
			4. Nav = 100000 - 45454.5454 - 4545.45454 + 54545.45448 = 104545.45454
		 */
		MarketName market = new MarketName("test_market");
		TestData data = new TestData("test_market").withDelistings(new Delisting(market, new Time(150), 120.0));
		StrategyExecutor executor = new StrategyExecutor().withCommissionRate(0.1).withInitialCash(100_000)
				.withTimeRange(new Time(1), new Time(160));

		Account account = executor.execute(data, new TestStrategy());

		assertEquals(0, account.getPositions().size());
		assertEquals(104_545.45454, account.getNAV(), 1e-6);
		List<AccountAction> actions = account.getAccountHistory().getActions();
		assertEquals(new AccountAction(new Time(150), new Trade(market, SELL, 120.0, 454.545454, 0.0)), actions.getLast());
	}

	@Test
	void shouldDropPendingOrdersForDelistedMarket() throws AccountActionException {
		MarketName market = new MarketName("test_market");
		Strategy strategy = new Strategy() {
			@Override
			public List<Order> afterClose(TimeMarketDataSet data, Account account) {
				if (data.getTime().equals(new Time(99))) {
					return arrayList(new Order(market, BUY, PERCENTAGE, 50, 1.0));
				}
				return arrayList();
			}
		};
		TestData data = new TestData("test_market").withDelistings(new Delisting(market, new Time(99), 99.0));

		Account account = new StrategyExecutor().execute(data, strategy);

		assertEquals(100_000, account.getNAV(), 0.001);
		assertTrue(account.getAccountHistory().getActions().stream().noneMatch(a -> a.actionType() instanceof Trade));
	}

	@Test
	void shouldIncludeCommissionWhenBuyingForEntireNav() throws AccountActionException {
		double initialCash = 100_000;
		double commissionRate = 0.1;
		MarketName market = new MarketName("test_market");
		Strategy strategy = new Strategy() {
			@Override
			public List<Order> afterClose(TimeMarketDataSet data, Account account) {
				if (data.getTime().equals(new Time(99))) {
					return arrayList(new Order(market, BUY, PERCENTAGE, 100, 1.0));
				}
				return arrayList();
			}
		};

		Account account = new StrategyExecutor()
				.withInitialCash(initialCash)
				.withCommissionRate(commissionRate)
				.withTimeRange(new Time(99), new Time(100))
				.execute(new TestData(market.name()), strategy);

		Trade buy = account.getAccountHistory().getActions().stream()
				.map(AccountAction::actionType)
				.filter(Trade.class::isInstance)
				.map(Trade.class::cast)
				.filter(trade -> trade.direction() == BUY)
				.findFirst()
				.orElseThrow();

		// 100000 / (100 * 1.1), rounded down to 0.000001
		assertEquals(909.090909, buy.quantity());
		assertEquals(9_090.90909, buy.commission(), 1e-6);
		assertTrue(buy.quantity() * buy.price() + buy.commission() <= initialCash);
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

	@Test
	void shouldRejectSecondExecution() throws AccountActionException {
		StrategyExecutor executor = new StrategyExecutor();
		TestData marketData = new TestData("test_market");
		executor.execute(marketData, new TestStrategy());

		assertThrows(IllegalStateException.class, () -> executor.execute(marketData, new TestStrategy()));
	}
}
