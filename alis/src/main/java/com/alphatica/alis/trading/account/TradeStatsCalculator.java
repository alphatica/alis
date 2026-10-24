package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.market.MarketName;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

final class TradeStatsCalculator {

	private static final TradeStats EMPTY_STATS = new TradeStats(0, 0, 0, 0, 0, 0, 0, 0);

	private TradeStatsCalculator() {
	}

	static TradeStats calculate(List<PositionHistoryRecord> results) {
		if (results.isEmpty()) {
			return EMPTY_STATS;
		}
		Accumulator accumulator = new Accumulator();
		results.forEach(accumulator::add);
		return accumulator.toTradeStats();
	}

	static Map<MarketName, TradeStats> calculateByMarket(List<PositionHistoryRecord> results) {
		return results.stream().collect(Collectors.groupingBy(PositionHistoryRecord::marketName,
				Collectors.collectingAndThen(Collectors.toList(), TradeStatsCalculator::calculate)));
	}

	static String biggestWin(List<PositionHistoryRecord> results) {
		double biggestPercent = 0;
		MarketName market = null;
		for (PositionHistoryRecord result : results) {
			if (result.getNetProfitPercent() > biggestPercent) {
				biggestPercent = result.getNetProfitPercent();
				market = result.marketName();
			}
		}
		return format("%s %.0f %%", market, biggestPercent);
	}

	static String biggestLoss(List<PositionHistoryRecord> results) {
		double biggestPercent = 0;
		MarketName market = null;
		for (PositionHistoryRecord result : results) {
			if (result.getNetProfitPercent() < biggestPercent) {
				biggestPercent = result.getNetProfitPercent();
				market = result.marketName();
			}
		}
		return format("%s %.0f %%", market, biggestPercent);
	}

	private static final class Accumulator {

		private double sumWinPercent;
		private int winCount;
		private double sumLossPercent;
		private int lossCount;
		private int trades;
		private int totalTradesLength;

		private void add(PositionHistoryRecord result) {
			double profitValue = result.getNetProfitCash();
			if (profitValue > 0) {
				winCount++;
				sumWinPercent += result.getNetProfitPercent();
			} else if (profitValue < 0) {
				lossCount++;
				sumLossPercent += result.getNetProfitPercent();
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
