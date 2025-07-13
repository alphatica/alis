package com.alphatica.alis.trading.optimizer;

import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ParamsStepsSet {

	private final Map<String, ParamSteps> paramStepsMap = new TreeMap<>();

	public Map<String, ParamSteps> getParamStepsMap() {
		return paramStepsMap;
	}

	public void addParamSteps(String field, ParamSteps paramSteps) {
		paramStepsMap.put(field, paramSteps);
	}

	public long computePermutations() {
		long permutations = 1;
		for (ParamSteps paramSteps : paramStepsMap.values()) {
			permutations *= paramSteps.size();
		}
		return permutations;
	}

	public Map<String, Object> getRandomValues() {
		return paramStepsMap.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getRandom()));
	}

}
