package com.alphatica.alis.charting;

import java.util.List;

record ChartModel<X extends Comparable<X>>(
		List<ChartPane<X>> panes,
		ChartSettings settings) {
}
