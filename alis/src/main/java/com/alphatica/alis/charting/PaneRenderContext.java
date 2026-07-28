package com.alphatica.alis.charting;

record PaneRenderContext<X extends Comparable<X>>(
		ChartPane<X> pane,
		int width,
		int height,
		XAxisLayout<X> xAxisLayout,
		boolean lastPane,
		String xAxisTitle,
		int marginRight,
		double plotContentSize,
		boolean multiplePanes) {
}
