package com.alphatica.alis.charting;

import java.util.ArrayList;
import java.util.List;

final class PaneHeightCalculator {

	private PaneHeightCalculator() {
	}

	static List<Integer> calculate(List<Double> weights, int totalHeight) {
		if (totalHeight < 0) {
			throw new IllegalArgumentException("totalHeight must not be negative");
		}
		if (weights.isEmpty()) {
			return List.of();
		}

		double largestWeight = weights.stream()
				.peek(PaneHeightCalculator::validateWeight)
				.mapToDouble(Double::doubleValue)
				.max()
				.orElseThrow();
		double normalizedSum = weights.stream()
				.mapToDouble(weight -> weight / largestWeight)
				.sum();

		List<Integer> heights = new ArrayList<>(weights.size());
		int remainingHeight = totalHeight;
		for (int index = 0; index < weights.size() - 1; index++) {
			double normalizedWeight = weights.get(index) / largestWeight;
			long roundedHeight = Math.round(totalHeight * normalizedWeight / normalizedSum);
			int height = (int) Math.min(roundedHeight, remainingHeight);
			heights.add(height);
			remainingHeight -= height;
		}
		heights.add(remainingHeight);
		return List.copyOf(heights);
	}

	private static void validateWeight(double weight) {
		if (!Double.isFinite(weight) || weight <= 0.0) {
			throw new IllegalArgumentException("weight must be finite and greater than zero");
		}
	}
}
