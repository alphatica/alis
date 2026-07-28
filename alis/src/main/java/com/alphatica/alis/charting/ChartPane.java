package com.alphatica.alis.charting;

import java.util.List;

record ChartPane<X extends Comparable<X>>(
		Scale scale,
		String title,
		List<LineChartData<X>> lines,
		PaneSettings settings) {
}
