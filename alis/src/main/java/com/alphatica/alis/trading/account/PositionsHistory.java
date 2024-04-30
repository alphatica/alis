package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.market.MarketName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PositionsHistory {
	private final Map<MarketName, List<PositionResult>> results;

	public PositionsHistory() {
		this.results = HashMap.newHashMap(1024);
	}

	public void add(MarketName marketName, PositionResult result) {
		results.computeIfAbsent(marketName, k -> new ArrayList<>()).add(result);
	}

	public TradingStats getStats() {
		return calcStats(results.values().stream().flatMap(Collection::stream).toList());
	}

	public Map<MarketName, TradingStats> getAllStats() {
		return results.entrySet().stream().collect(Collectors.toMap(
							  Map.Entry::getKey,
							  entry -> calcStats(entry.getValue())
					  ));
	}

	private TradingStats calcStats(List<PositionResult> list) {
		double sumWinPercent = 0;
		int winCount = 0;
		double sumLossPercent = 0;
		int lossCount = 0;
		for(PositionResult result : list) {
			if (result.profitValue() > 0) {
				winCount++;
				sumWinPercent += result.profitPercent();
			}
			if (result.profitValue() < 0) {
				lossCount++;
				sumLossPercent += result.profitPercent();
			}
		}
		int trades = list.size();
		double accuracy;
		if (lossCount > 0) {
			accuracy = ((double) winCount / (double) (winCount + lossCount)) * 100.0;
		} else {
			accuracy = 100.0;
		}
		double averageWinPercent = 0;
		if (winCount > 0) {
			averageWinPercent = sumWinPercent / winCount;
		}
		double averageLossPercent = 0;
		if (lossCount > 0) {
			averageLossPercent = sumLossPercent / lossCount;
		}

		double profitFactor = Double.NaN;
		double overallProfitPercent = sumWinPercent + sumLossPercent;
		if (sumLossPercent < 0.0) {
			profitFactor = sumWinPercent / -sumLossPercent;
		}
		return new TradingStats(overallProfitPercent, accuracy, averageWinPercent, averageLossPercent, profitFactor, trades);
	}
}
