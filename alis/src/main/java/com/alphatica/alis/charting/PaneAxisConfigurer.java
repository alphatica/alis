package com.alphatica.alis.charting;

import org.knowm.xchart.XYChart;

import java.util.List;
import java.util.Locale;

final class PaneAxisConfigurer<X extends Comparable<X>> {

	private final PaneRenderContext<X> context;
	private final PaneValueTransformer valueTransformer;

	PaneAxisConfigurer(PaneRenderContext<X> context, PaneValueTransformer valueTransformer) {
		this.context = context;
		this.valueTransformer = valueTransformer;
	}

	void configure(XYChart chart) {
		XRange range = xRange();
		chart.getStyler().setXAxisMin(range.min());
		chart.getStyler().setXAxisMax(range.max());
		chart.setCustomXAxisTickLabelsFormatter(value -> formatXLabel(value, context.xAxisLayout().labels()));
		chart.setCustomYAxisTickLabelsFormatter(value -> formatYLabel(valueTransformer.inverse(value)));
	}

	XRange xRange() {
		int labelsCount = context.xAxisLayout().labels().size();
		if (labelsCount == 1) {
			return new XRange(-0.5, 0.5);
		}
		return new XRange(0.0, Math.max(1.0, labelsCount - 1.0));
	}

	private static <X> String formatXLabel(Double value, List<X> xValues) {
		long index = Math.round(value);
		if (Math.abs(value - index) > 0.000_001 || index < 0 || index >= xValues.size()) {
			return "";
		}
		return xValues.get((int) index).toString();
	}

	private static String formatYLabel(double value) {
		return String.format(Locale.ROOT, "%.0f", value);
	}
}
