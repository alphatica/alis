package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.market.MarketName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountHistoryTest {

	@Test
	void shouldReturnZeroStatsWhenThereAreNoTrades() {
		AccountHistory history = new AccountHistory(100_000);

		TradeStats stats = history.getStats();

		assertEquals(new TradeStats(0, 0, 0, 0, 0, 0, 0, 0), stats);
	}

	@Test
	void shouldCalculateStatsForWinningTrades() {
		AccountHistory history = new AccountHistory(100_000);
		history.add(new MarketName("WIN"), new PositionResult(100, 10, 4));
		history.add(new MarketName("WIN"), new PositionResult(50, 20, 6));

		TradeStats stats = history.getStats();

		assertStats(stats, new TradeStats(15, 100, 15, 0, Double.NaN, 0, 2, 5));
	}

	@Test
	void shouldCalculateStatsForLosingTrades() {
		AccountHistory history = new AccountHistory(100_000);
		history.add(new MarketName("LOSS"), new PositionResult(-10, -10, 2));
		history.add(new MarketName("LOSS"), new PositionResult(-20, -20, 4));

		TradeStats stats = history.getStats();

		assertStats(stats, new TradeStats(-15, 0, 0, -15, 0, -1, 2, 3));
	}

	@Test
	void shouldCalculateMixedStatsAndIncludeBreakEvenTradeInOverallAverages() {
		AccountHistory history = new AccountHistory(100_000);
		MarketName market = new MarketName("MIXED");
		history.add(market, new PositionResult(10, 20, 5));
		history.add(market, new PositionResult(5, 10, 3));
		history.add(market, new PositionResult(-2, -5, 2));
		history.add(market, new PositionResult(0, 99, 2));

		TradeStats stats = history.getStats();

		assertStats(stats, new TradeStats(6.25, 200.0 / 3.0, 15, -5, 6, 1, 4, 3));
	}

	@Test
	void shouldTreatOnlyBreakEvenTradesAsPerfectAccuracyWithoutProfit() {
		AccountHistory history = new AccountHistory(100_000);
		history.add(new MarketName("FLAT"), new PositionResult(0, 50, 7));

		TradeStats stats = history.getStats();

		assertStats(stats, new TradeStats(0, 100, 0, 0, Double.NaN, 0, 1, 7));
	}

	@Test
	void shouldCalculateStatsForEachMarketSeparately() {
		AccountHistory history = new AccountHistory(100_000);
		MarketName winningMarket = new MarketName("WIN");
		MarketName losingMarket = new MarketName("LOSS");
		history.add(winningMarket, new PositionResult(10, 25, 4));
		history.add(losingMarket, new PositionResult(-10, -15, 8));

		Map<MarketName, TradeStats> allStats = history.getAllStats();

		assertStats(allStats.get(winningMarket), new TradeStats(25, 100, 25, 0, Double.NaN, 0, 1, 4));
		assertStats(allStats.get(losingMarket), new TradeStats(-15, 0, 0, -15, 0, -1, 1, 8));
	}

	private static void assertStats(TradeStats stats, TradeStats expected) {
		assertEquals(expected.profitPerTrade(), stats.profitPerTrade(), 0.000_001);
		assertEquals(expected.accuracy(), stats.accuracy(), 0.000_001);
		assertEquals(expected.averageWinPercent(), stats.averageWinPercent(), 0.000_001);
		assertEquals(expected.averageLossPercent(), stats.averageLossPercent(), 0.000_001);
		if (Double.isNaN(expected.profitFactor())) {
			assertTrue(Double.isNaN(stats.profitFactor()));
		} else {
			assertEquals(expected.profitFactor(), stats.profitFactor(), 0.000_001);
		}
		assertEquals(expected.expectancy(), stats.expectancy(), 0.000_001);
		assertEquals(expected.trades(), stats.trades());
		assertEquals(expected.averageTradeLength(), stats.averageTradeLength(), 0.000_001);
	}
}
