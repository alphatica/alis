package com.alphatica.alis.trading.account;

import java.util.List;

final class TradeStatsCalculator {

	private static final TradeStats EMPTY_STATS = new TradeStats(0, 0, 0, 0, 0, 0, 0, 0);

	private TradeStatsCalculator() {
	}

	static TradeStats calculate(List<PositionResult> results) {
		if (results.isEmpty()) {
			return EMPTY_STATS;
		}
		Accumulator accumulator = new Accumulator();
		results.forEach(accumulator::add);
		return accumulator.toTradeStats();
	}

	private static final class Accumulator {

		private double sumWinPercent;
		private int winCount;
		private double sumLossPercent;
		private int lossCount;
		private int trades;
		private int totalTradesLength;

		private void add(PositionResult result) {
			if (result.profitValue() > 0) {
				winCount++;
				sumWinPercent += result.profitPercent();
			} else if (result.profitValue() < 0) {
				lossCount++;
				sumLossPercent += result.profitPercent();
			}
			trades++;
			totalTradesLength += result.tradeLength();
		}

		private TradeStats toTradeStats() {
			double averageWinPercent = average(sumWinPercent, winCount);
			double averageLossPercent = average(sumLossPercent, lossCount);
			return new TradeStats(
					(sumWinPercent + sumLossPercent) / trades,
					accuracy(),
					averageWinPercent,
					averageLossPercent,
					profitFactor(),
					expectancy(averageWinPercent, averageLossPercent),
					trades,
					(double) totalTradesLength / trades);
		}

		private double accuracy() {
			if (lossCount == 0) {
				return 100.0;
			}
			return ((double) winCount / (winCount + lossCount)) * 100.0;
		}

		private double profitFactor() {
			if (sumLossPercent < 0.0) {
				return sumWinPercent / -sumLossPercent;
			}
			return Double.NaN;
		}

		private double expectancy(double averageWinPercent, double averageLossPercent) {
			if (averageLossPercent >= 0.0) {
				return 0.0;
			}
			double winProbability = (double) winCount / trades;
			return ((1 + averageWinPercent / -averageLossPercent) * winProbability) - 1;
		}

		private static double average(double sum, int count) {
			if (count == 0) {
				return 0.0;
			}
			return sum / count;
		}
	}
}
