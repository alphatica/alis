package com.alphatica.alis.trading.signalcheck.scoregenerator;

import com.alphatica.alis.trading.signalcheck.AllocationReplayResult;
import com.alphatica.alis.trading.signalcheck.SignalExecutionResult;

@FunctionalInterface
public interface ScoreCalculator {
	double calculate(SignalExecutionResult execution, AllocationReplayResult replay);
}
