package com.alphatica.alis.charting;

import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.SeriesMarkers;

import java.util.ArrayList;
import java.util.List;

final class PaneSeriesRenderer<X extends Comparable<X>> {

	private final PaneRenderContext<X> context;
	private final PaneValueTransformer valueTransformer;

	PaneSeriesRenderer(PaneRenderContext<X> context, PaneValueTransformer valueTransformer) {
		this.context = context;
		this.valueTransformer = valueTransformer;
	}

	void addSeries(XYChart chart, XRange range) {
		addDataSeries(chart);
		addHorizontalSeries(chart, range);
		addEmptyChartAnchor(chart);
	}

	private void addDataSeries(XYChart chart) {
		List<LineChartData<X>> lines = context.pane().lines();
		for (int index = 0; index < lines.size(); index++) {
			LineChartData<X> dataLine = lines.get(index);
			List<Double> xData = new ArrayList<>();
			List<Double> yData = new ArrayList<>();
			dataLine.getData().forEach((x, values) -> addPoints(
					context.xAxisLayout().positions().get(x), values, xData, yData));
			if (!xData.isEmpty()) {
				XYSeries series = chart.addSeries("data-" + index, xData, yData);
				configureDataSeries(series, dataLine);
			}
		}
	}

	private void addPoints(Double x, List<Double> values, List<Double> xData, List<Double> yData) {
		for (Double value : values) {
			if (value != null && Double.isFinite(value)) {
				xData.add(x);
				yData.add(valueTransformer.transform(value));
			}
		}
	}

	private static <X extends Comparable<X>> void configureDataSeries(
			XYSeries series,
			LineChartData<X> dataLine) {
		series.setMarker(SeriesMarkers.SQUARE);
		series.setXYSeriesRenderStyle(dataLine.isConnectPoints()
				? XYSeries.XYSeriesRenderStyle.Line
				: XYSeries.XYSeriesRenderStyle.Scatter);
		series.setLineStyle(dataLine.isConnectPoints() ? SeriesLines.SOLID : SeriesLines.NONE);
		series.setLineWidth(3.0f);
		configureLegendEntry(series, dataLine.getName());
	}

	private void addHorizontalSeries(XYChart chart, XRange range) {
		List<HorizontalLine> horizontalLines = context.pane().settings().horizontalLines();
		for (int index = 0; index < horizontalLines.size(); index++) {
			HorizontalLine horizontalLine = horizontalLines.get(index);
			if (!Double.isFinite(horizontalLine.value())) {
				continue;
			}
			double transformedValue = valueTransformer.transform(horizontalLine.value());
			XYSeries series = chart.addSeries(
					"horizontal-" + index,
					List.of(range.min(), range.max()),
					List.of(transformedValue, transformedValue));
			series.setXYSeriesRenderStyle(XYSeries.XYSeriesRenderStyle.Line);
			series.setMarker(SeriesMarkers.NONE);
			series.setLineStyle(SeriesLines.SOLID);
			series.setLineWidth(3.0f);
			configureLegendEntry(series, horizontalLine.name());
		}
	}

	private static void configureLegendEntry(XYSeries series, String label) {
		series.setShowInLegend(label != null);
		if (label != null) {
			series.setLabel(label);
		}
	}

	private static void addEmptyChartAnchor(XYChart chart) {
		if (!chart.getSeriesCollection().isEmpty()) {
			return;
		}
		XYSeries anchor = chart.addSeries("empty-chart", List.of(0.0, 1.0), List.of(0.0, 0.0));
		anchor.setShowInLegend(false);
		anchor.setMarker(SeriesMarkers.NONE);
		anchor.setLineStyle(SeriesLines.NONE);
	}
}
