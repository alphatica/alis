package com.alphatica.alis.studio.logic.trading.optimize;

import com.alphatica.alis.trading.optimizer.Optimizer;
import com.alphatica.alis.trading.optimizer.ParametersSelection;
import com.alphatica.alis.trading.strategy.Strategy;

public final class MaxPermutations {

	private MaxPermutations() {
	}

	public static long defaultFor(Strategy strategy, ParametersSelection parametersSelection) {
		return switch (parametersSelection) {
			case FULL_PERMUTATION -> Optimizer.computeAllPermutations(strategy);
			case GENETIC, RANDOM -> 10_000L;
		};
	}
}
