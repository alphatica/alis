package com.alphatica.alis.trading.strategy.optimizer.paramsselector;

import com.alphatica.alis.trading.strategy.optimizer.OptimizerScore;
import com.alphatica.alis.trading.strategy.optimizer.ParametersSelection;
import com.alphatica.alis.trading.strategy.optimizer.ParamsStepsSet;

import java.util.Map;

public abstract class ParamsSelector {

	protected final ParamsStepsSet paramsStepsSet;

	protected ParamsSelector(ParamsStepsSet paramsStepsSet) {
		this.paramsStepsSet = paramsStepsSet;
	}

	public abstract Map<String, Object> next();

	public void registerScore(OptimizerScore newScore) {}

	public static ParamsSelector get(ParametersSelection selection, ParamsStepsSet paramsStepsSet) {
		return switch (selection) {
			case FULL_PERMUTATION -> new FullPermutationParamsSelector(paramsStepsSet);
			case GENETIC -> new GeneticParamsSelector(paramsStepsSet);
			case RANDOM -> new RandomParamsSelector(paramsStepsSet);
		};
	}

}
