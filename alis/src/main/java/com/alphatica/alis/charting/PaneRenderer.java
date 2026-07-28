package com.alphatica.alis.charting;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;

import java.awt.image.BufferedImage;

final class PaneRenderer<X extends Comparable<X>> {

	private final PaneRenderContext<X> context;

	PaneRenderer(PaneRenderContext<X> context) {
		this.context = context;
	}

	BufferedImage createImage() {
		return BitmapEncoder.getBufferedImage(createXChart());
	}

	private XYChart createXChart() {
		ChartPane<X> pane = context.pane();
		XYChart chart = new XYChartBuilder()
				.width(context.width())
				.height(context.height())
				.title(pane.title() == null ? "" : pane.title())
				.xAxisTitle(context.lastPane() && context.xAxisTitle() != null ? context.xAxisTitle() : "")
				.yAxisTitle(pane.settings().yAxisTitle() == null ? "" : pane.settings().yAxisTitle())
				.build();

		PaneValueTransformer valueTransformer = new PaneValueTransformer(pane.scale());
		PaneAxisConfigurer<X> axisConfigurer = new PaneAxisConfigurer<>(context, valueTransformer);
		new PaneStyleConfigurer<>(context).configure(chart);
		new PaneSeriesRenderer<>(context, valueTransformer).addSeries(chart, axisConfigurer.xRange());
		axisConfigurer.configure(chart);
		chart.getStyler().setLegendVisible(
				chart.getSeriesCollection().stream().anyMatch(XYSeries::isShowInLegend));
		return chart;
	}
}
