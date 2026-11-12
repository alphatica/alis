package com.alphatica.alis.studio.logic.stats.edge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record EdgeSeries(String name, List<Double> values) {

	static EdgeSeries of(String name, double[] values) {
		List<Double> result = new ArrayList<>(values.length);
		for (double value : values) {
			result.add(Double.isFinite(value) ? value : null);
		}
		return new EdgeSeries(name, Collections.unmodifiableList(result));
	}
}
