package com.alphatica.alis.charting;

import java.util.List;
import java.util.Map;

record XAxisLayout<X extends Comparable<X>>(
		List<X> labels,
		Map<X, Double> positions) {
}
