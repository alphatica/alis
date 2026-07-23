package com.alphatica.alis.trading.signalcheck.scoregenerator;

import com.alphatica.alis.trading.signalcheck.AllocationReplayResult;
import com.alphatica.alis.trading.signalcheck.SelectedTrade;
import com.alphatica.alis.trading.signalcheck.SignalExecutionResult;

import static com.alphatica.alis.tools.java.NumberTools.percentChange;

public final class ArithmeticAverageProfitPerBarScoreCalculator implements ScoreCalculator {
	@Override
	public double calculate(SignalExecutionResult execution, AllocationReplayResult replay) {
		double weightedProfit = 0.0;
		double allocatedBars = 0.0;
		for (SelectedTrade selectedTrade : replay.acceptedTrades()) {
			var opportunity = selectedTrade.opportunity();
			double allocation = selectedTrade.actualAllocation();
			weightedProfit += percentChange(opportunity.effectiveOpenPrice(),
					opportunity.effectiveClosePrice()) * allocation;
			allocatedBars += opportunity.bars() * allocation;
		}
		return allocatedBars == 0.0 ? Double.NaN : weightedProfit / allocatedBars;
	}
}
