package com.alphatica.alis.trading.signalcheck.scoregenerator;

import com.alphatica.alis.trading.signalcheck.AllocationReplayResult;
import com.alphatica.alis.trading.signalcheck.SelectedTrade;
import com.alphatica.alis.trading.signalcheck.SignalExecutionResult;

import static com.alphatica.alis.tools.java.NumberTools.percentChange;

public final class CapacityAdjustedScoreCalculator implements ScoreCalculator {
	@Override
	public double calculate(SignalExecutionResult execution, AllocationReplayResult replay) {
		if (!Double.isFinite(replay.maxAllocation())) {
			throw new IllegalArgumentException("capacity-adjusted score requires a finite maxAllocation");
		}
		double weightedProfit = 0.0;
		for (SelectedTrade selectedTrade : replay.acceptedTrades()) {
			var opportunity = selectedTrade.opportunity();
			weightedProfit += percentChange(opportunity.effectiveOpenPrice(),
					opportunity.effectiveClosePrice()) * selectedTrade.actualAllocation();
		}
		double capacityBars = execution.executionTimes().size() * replay.maxAllocation();
		return capacityBars == 0.0 ? Double.NaN : weightedProfit / capacityBars;
	}
}
