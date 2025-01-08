package com.alphatica.alis.trading.strategy.optimizer.paramsselector;

import com.alphatica.alis.trading.strategy.optimizer.ParamSteps;
import com.alphatica.alis.trading.strategy.optimizer.ParamsStepsSet;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static java.util.Collections.emptyMap;

public class FullPermutationParamsSelector extends ParamsSelector {

	private final SortedMap<String, Integer> nextIndices = new TreeMap<>();
	private boolean finished = false;

	protected FullPermutationParamsSelector(ParamsStepsSet paramsStepsSet) {
		super(paramsStepsSet);
		for(String name: paramsStepsSet.getParamStepsMap().keySet()) {
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
		String lastName = nextIndices.lastKey();
		if (nextIndices.lastEntry().getValue() == paramsStepsSet.getParamStepsMap().get(lastName).size() -1) {
			finished = true;
		}
		for(Map.Entry<String, Integer> nextEntry: nextIndices.entrySet()) {
			if (nextEntry.getValue() < paramsStepsSet.getParamStepsMap().get(nextEntry.getKey()).size() -1) {
				nextEntry.setValue(nextEntry.getValue() + 1);
				return;
			}
			nextEntry.setValue(0);
		}
	}

	private Map<String, Object> getNext() {
		Map<String, Object> next = new HashMap<>();
		for(Map.Entry<String, ParamSteps> param: paramsStepsSet.getParamStepsMap().entrySet()) {
			next.put(param.getKey(), param.getValue().values()[nextIndices.get(param.getKey())]);
		}
		return next;
	}


}

