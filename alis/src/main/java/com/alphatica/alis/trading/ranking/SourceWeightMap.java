package com.alphatica.alis.trading.ranking;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SourceWeightMap {
	private final Map<String, Double> weights = new HashMap<>();
	public void addWeight(String source, Double weight) {
		weights.put(source, weight);
	}

	public double getWeight(String source) {
		return weights.get(source);
	}

	public Set<String> sources() {
		return weights.keySet();
	}

	public Map<String, Double> getContent() {
		return weights;
	}
}
