package com.alphatica.alis.trading.optimizer.paramsselector;

import com.alphatica.alis.trading.optimizer.ParamsStepsSet;

import java.util.Map;

public class RandomParamsSelector extends ParamsSelector {

	public RandomParamsSelector(ParamsStepsSet paramsStepsSet) {
		super(paramsStepsSet);
	}

	@Override
	public Map<String, Object> next() {
		return paramsStepsSet.getRandomValues();
	}

}
