package com.alphatica.alis.trading.ranking;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.MarketScore;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.PositionEntry;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.order.Direction;
import com.alphatica.alis.trading.order.Order;
import org.junit.jupiter.api.AssertionFailureBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.alphatica.alis.trading.order.OrderSize.PERCENTAGE;
import static org.junit.jupiter.api.Assertions.*;

class RankingTraderTest {

	@Test
	void shouldThrowExceptionWhenMaxMarketsTooHigh() {
		assertThrows(IllegalArgumentException.class, () -> new RankingTrader(data -> List.of(), 101, 0));
	}

	@Test
	void shouldThrowExceptionWhenMaxMarketsTooLow() {
		assertThrows(IllegalArgumentException.class, () -> new RankingTrader(data -> List.of(), 0, 0));
	}

	@Test
	void shouldDoNothingWhenNoPositionsAndNoRanking() {
		RankingProvider rankingProvider = data -> List.of();

		Account account = new Account(100_000);
		var trader = new RankingTrader(rankingProvider, 10, 0);
		var orders = trader.afterClose(null, account);
		assertTrue(orders.isEmpty());
	}

	@Test
	void shouldNotBuyMoreMarketsThanMaxMarkets() {
		RankingProvider rankingProvider = data -> List.of(
				new MarketScore(new MarketName("1"), 5),
				new MarketScore(new MarketName("2"), 4),
				new MarketScore(new MarketName("3"), 3),
				new MarketScore(new MarketName("4"), 2),
				new MarketScore(new MarketName("5"), 1)
		);
		var trader = new RankingTrader(rankingProvider, 4, 0);
		Account account = new Account(5_000);
		var orders = trader.afterClose(null, account);
		assertEquals(4, orders.size());
		assertHasBuyOrder(orders, new MarketName("1"), 5, 25);
		assertHasBuyOrder(orders, new MarketName("2"), 4, 25);
		assertHasBuyOrder(orders, new MarketName("3"), 3, 25);
		assertHasBuyOrder(orders, new MarketName("4"), 2, 25);
	}

	@Test
	void shouldDoNothingWhenPositionsAndRankingMatch() throws AccountActionException {
		RankingProvider rankingProvider = data -> List.of(
				new MarketScore(new MarketName("1"), 1),
				new MarketScore(new MarketName("2"), 1),
				new MarketScore(new MarketName("3"), 1),
				new MarketScore(new MarketName("4"), 1),
				new MarketScore(new MarketName("5"), 1)
		);
		var trader = new RankingTrader(rankingProvider, 5, 0);

		Account account = new Account(5_000);
		account.addPosition(new MarketName("1"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("2"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("3"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("4"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("5"), new PositionEntry(new Time(1), 100, 10), 0.0);

		List<Order> orders = trader.afterClose(null, account);
		assertTrue(orders.isEmpty());
	}

	@Test
	void shouldReplaceMarketsNotInTop() throws AccountActionException {
		RankingProvider rankingProvider = data -> List.of(
				new MarketScore(new MarketName("1"), 7),
				new MarketScore(new MarketName("2"), 6),
				new MarketScore(new MarketName("3"), 5),
				new MarketScore(new MarketName("4"), 4),
				new MarketScore(new MarketName("5"), 3),
				new MarketScore(new MarketName("6"), 2),
				new MarketScore(new MarketName("7"), 1)
		);
		var trader = new RankingTrader(rankingProvider, 5, 0);

		Account account = new Account(5_000);
		account.addPosition(new MarketName("1"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("2"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("3"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("6"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("7"), new PositionEntry(new Time(1), 100, 10), 0.0);

		List<Order> orders = trader.afterClose(null, account);
		assertEquals(4, orders.size());
		assertHasSellOrder(orders, new MarketName("6"));
		assertHasSellOrder(orders, new MarketName("7"));
		assertHasBuyOrder(orders, new MarketName("4"), 4, 20);
		assertHasBuyOrder(orders, new MarketName("5"), 3, 20);
	}

	@Test
	void shouldSellLastMarketWhenTooManyPositions() throws AccountActionException {
		RankingProvider rankingProvider = data -> List.of(
				new MarketScore(new MarketName("1"), 6),
				new MarketScore(new MarketName("2"), 5),
				new MarketScore(new MarketName("3"), 4),
				new MarketScore(new MarketName("4"), 3),
				new MarketScore(new MarketName("5"), 2),
				new MarketScore(new MarketName("6"), 1)
		);
		var trader = new RankingTrader(rankingProvider, 5, 0);

		Account account = new Account(6_000);
		account.addPosition(new MarketName("1"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("2"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("3"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("4"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("5"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("6"), new PositionEntry(new Time(1), 100, 10), 0.0);

		List<Order> orders = trader.afterClose(null, account);
		assertEquals(1, orders.size());
		assertHasSellOrder(orders, new MarketName("6"));
	}

	@Test
	void sellAllWhenNoRankingButHasPositions() throws AccountActionException {
		RankingProvider rankingProvider = data -> List.of();
		Account account = new Account(6_000);
		account.addPosition(new MarketName("1"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("2"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("3"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("4"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("5"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("6"), new PositionEntry(new Time(1), 100, 10), 0.0);
		var trader = new RankingTrader(rankingProvider, 5, 0);
		List<Order> orders = trader.afterClose(null, account);

		assertEquals(6, orders.size());
		assertHasSellOrder(orders, new MarketName("1"));
		assertHasSellOrder(orders, new MarketName("2"));
		assertHasSellOrder(orders, new MarketName("3"));
		assertHasSellOrder(orders, new MarketName("4"));
		assertHasSellOrder(orders, new MarketName("5"));
		assertHasSellOrder(orders, new MarketName("6"));
	}

	@Test
	void shouldBuyAllFromRankingWhenNoPositions() {
		RankingProvider rankingProvider = data -> List.of(
				new MarketScore(new MarketName("1"), 5),
				new MarketScore(new MarketName("2"), 4),
				new MarketScore(new MarketName("3"), 3)
		);
		var trader = new RankingTrader(rankingProvider, 5, 0);
		Account account = new Account(5_000);
		List<Order> orders = trader.afterClose(null, account);
		assertEquals(3, orders.size());
		assertHasBuyOrder(orders, new MarketName("1"), 5, 20);
		assertHasBuyOrder(orders, new MarketName("2"), 4, 20);
		assertHasBuyOrder(orders, new MarketName("3"), 3, 20);
	}

	@Test
	void shouldKeepMarketsThatAreWithinOffset() throws AccountActionException {
		RankingProvider rankingProvider = data -> List.of(
				new MarketScore(new MarketName("1"), 5),
				new MarketScore(new MarketName("2"), 4),
				new MarketScore(new MarketName("3"), 3),
				new MarketScore(new MarketName("4"), 2),
				new MarketScore(new MarketName("5"), 1)
		);
		Account account = new Account(6_000);
		account.addPosition(new MarketName("1"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("2"), new PositionEntry(new Time(1), 100, 10), 0.0);
		account.addPosition(new MarketName("5"), new PositionEntry(new Time(1), 100, 10), 0.0);
		var trader = new RankingTrader(rankingProvider, 2, 1); // should only have markets 2 and 3
		List<Order> orders = trader.afterClose(null, account);
		assertEquals(3, orders.size());
		assertHasSellOrder(orders, new MarketName("1"));
		assertHasSellOrder(orders, new MarketName("5"));
		assertHasBuyOrder(orders, new MarketName("3"), 3, 50);
	}

	private void assertHasBuyOrder(List<Order> orders, MarketName marketName, double priority, int percentage) {
		for(var order: orders) {
			if (order.direction() == Direction.BUY && order.market().equals(marketName) && order.sizeValue() == percentage && order.size() == PERCENTAGE && order.priority() == priority) {
				return;
			}
		}
		AssertionFailureBuilder.assertionFailure().message("Order buy for market '" + marketName + "' not found").buildAndThrow();
	}


	private void assertHasSellOrder(List<Order> orders, MarketName marketName) {
		for(var order: orders) {
			if (order.direction() == Direction.SELL && order.market().equals(marketName) && order.sizeValue() == 100 && order.size() == PERCENTAGE && order.priority() == 1.0) {
				return;
			}
		}
		AssertionFailureBuilder.assertionFailure().message("Order sell for market '" + marketName + "' not found").buildAndThrow();
	}
}