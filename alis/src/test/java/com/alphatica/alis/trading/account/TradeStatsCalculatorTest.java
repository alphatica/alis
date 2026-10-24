package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TradeStatsCalculatorTest {
	private final MarketName market = new MarketName("market");

	@Test
	void shouldReturnZeroStatsWhenThereAreNoTrades() {
		TradeStats stats = TradeStatsCalculator.calculate(List.of());

		assertEquals(new TradeStats(0, 0, 0, 0, 0, 0, 0, 0), stats);
	}

	@Test
	void shouldCalculateStatsForWinningTrades() {
		TradeStats stats = TradeStatsCalculator.calculate(List.of(record(10, 4), record(20, 6)));

		assertStats(stats, new TradeStats(15, 100, 15, 0, Double.NaN, 0, 2, 5));
	}

	@Test
	void shouldCalculateStatsForLosingTrades() {
		TradeStats stats = TradeStatsCalculator.calculate(List.of(record(-10, 2), record(-20, 4)));

		assertStats(stats, new TradeStats(-15, 0, 0, -15, 0, -1, 2, 3));
	}

	@Test
	void shouldCalculateMixedStatsAndIncludeBreakEvenTradeInOverallAverages() {
		TradeStats stats = TradeStatsCalculator.calculate(List.of(record(20, 5), record(10, 3), record(-5, 2), record(0, 2)));

		assertStats(stats, new TradeStats(6.25, 200.0 / 3.0, 15, -5, 6, 1, 4, 3));
	}

	@Test
	void shouldTreatOnlyBreakEvenTradesAsPerfectAccuracyWithoutProfit() {
		TradeStats stats = TradeStatsCalculator.calculate(List.of(record(0, 7)));

		assertStats(stats, new TradeStats(0, 100, 0, 0, Double.NaN, 0, 1, 7));
	}

	@Test
	void shouldClassifyWinsAndLossesAfterCommissions() {
		// 1% gross profit eaten by commissions
		PositionHistoryRecord record = new PositionHistoryRecord(market, new Time(1), new Time(2), 100, 101, 10, 5, 6, 3, 1, 1, false);

		TradeStats stats = TradeStatsCalculator.calculate(List.of(record));

		assertEquals(-1, record.getNetProfitCash(), 0.000_001);
		assertEquals(0, stats.accuracy(), 0.000_001);
		assertEquals(record.getNetProfitPercent(), stats.averageLossPercent(), 0.000_001);
	}

	/**
	 * Record with given profit percent and no commissions.
	 */
	private PositionHistoryRecord record(double profitPercent, int tradeLength) {
		return new PositionHistoryRecord(market, new Time(1), new Time(2), 100, 100 + profitPercent, 1, 0, 0, tradeLength, 1, 1, false);
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
