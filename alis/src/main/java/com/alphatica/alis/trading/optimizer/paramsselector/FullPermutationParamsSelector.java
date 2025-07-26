package com.alphatica.alis.trading.optimizer.paramsselector;

import com.alphatica.alis.trading.optimizer.ParamSteps;
import com.alphatica.alis.trading.optimizer.ParamsStepsSet;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.util.Collections.emptyMap;

public class FullPermutationParamsSelector extends ParamsSelector {

	private final SortedMap<String, Integer> nextIndices = new TreeMap<>();
	private volatile boolean finished = false;

	public FullPermutationParamsSelector(ParamsStepsSet paramsStepsSet) {
		super(paramsStepsSet);
		for (String name : paramsStepsSet.getParamStepsMap().keySet()) {
			nextIndices.put(name, 0);
		}
	}

	@Override
	public synchronized Map<String, Object> next() {
		if (finished) {
			return emptyMap();
		} else {
			Map<String, Object> next = getNext();
			iterateOverNext();
			return next;
		}
	}

	private void iterateOverNext() {
		boolean carry = true;
		for (Map.Entry<String, Integer> entry: nextIndices.entrySet()) {
			if (carry) {
				int currentIndex = nextIndices.get(entry.getKey());
				ParamSteps steps = paramsStepsSet.getParamStepsMap().get(entry.getKey());
				if (currentIndex < steps.size() - 1) {
					nextIndices.put(entry.getKey(), currentIndex + 1);
					carry = false;
				} else {
					nextIndices.put(entry.getKey(), 0);
				}
			}
		}
		if (carry) {
			finished = true;
		}
	}

	private Map<String, Object> getNext() {
		Map<String, Object> next = new HashMap<>();
		for (Map.Entry<String, ParamSteps> param : paramsStepsSet.getParamStepsMap().entrySet()) {
			next.put(param.getKey(), param.getValue().values()[nextIndices.get(param.getKey())]);
		}
		return next;
	}
}