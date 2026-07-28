package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.data.time.Time;

import java.util.Collections;
import java.util.List;

final class FuzzyStartTimeSelector {

	private FuzzyStartTimeSelector() {
	}

	static List<Time> select(List<Time> times, Time preferredStartTime, int maxOptimizations) {
		if (times.isEmpty() || maxOptimizations <= 0) {
			return List.of();
		}
		int optimizations = Math.min(maxOptimizations, times.size());
		int index = Collections.binarySearch(times, preferredStartTime);
		if (index < 0) {
			index = -index - 1;
		}
		int preferredIndex = Math.min(index, times.size() - 1);
		int lastPossibleStartIndex = times.size() - optimizations;
		int startIndex = Math.clamp(preferredIndex - optimizations / 2, 0, lastPossibleStartIndex);
		return times.subList(startIndex, startIndex + optimizations);
	}
}
