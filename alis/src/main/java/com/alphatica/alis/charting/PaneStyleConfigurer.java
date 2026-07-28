package com.alphatica.alis.charting;

import org.knowm.xchart.XYChart;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.XYStyler;

import java.awt.Color;

final class PaneStyleConfigurer<X extends Comparable<X>> {

	private static final int POINT_SIZE = 4;
	private final PaneRenderContext<X> context;

	PaneStyleConfigurer(PaneRenderContext<X> context) {
		this.context = context;
	}

	void configure(XYChart chart) {
		XYStyler styler = chart.getStyler();
		styler.setAntiAlias(true);
		styler.setTextAntiAlias(true);
		styler.setChartBackgroundColor(Color.BLACK);
		styler.setPlotBackgroundColor(Color.BLACK);
		styler.setChartFontColor(Color.LIGHT_GRAY);
		styler.setChartTitleFontColor(Color.WHITE);
		styler.setChartTitleFont(PaneTheme.TITLE_FONT);
		styler.setChartTitleVisible(context.pane().title() != null);
		styler.setAxisTitleFont(PaneTheme.LABEL_FONT);
		styler.setAxisTickLabelsFont(PaneTheme.LABEL_FONT);
		styler.setAxisTickLabelsColor(Color.LIGHT_GRAY);
		styler.setAxisTickMarksColor(Color.LIGHT_GRAY);
		styler.setXAxisTitleColor(Color.LIGHT_GRAY);
		styler.setYAxisTitleColor(Color.LIGHT_GRAY);
		styler.setPlotBorderVisible(true);
		styler.setPlotBorderColor(Color.LIGHT_GRAY);
		styler.setPlotGridLinesVisible(true);
		styler.setPlotGridLinesColor(Color.DARK_GRAY);
		styler.setSeriesColors(PaneTheme.SERIES_COLORS);
		styler.setMarkerSize(POINT_SIZE);
		styler.setLegendPosition(Styler.LegendPosition.OutsideE);
		styler.setLegendBackgroundColor(Color.BLACK);
		styler.setLegendBorderColor(Color.LIGHT_GRAY);
		styler.setLegendFont(PaneTheme.LABEL_FONT);
		styler.setPlotContentSize(context.plotContentSize());
		styler.setXAxisMaxLabelCount(Math.clamp(context.xAxisLayout().labels().size(), 1, 11));
		styler.setXAxisTickMarkSpacingHint(Math.max(
				1,
				(PaneLayoutCalculator.WIDTH - PaneLayoutCalculator.MARGIN_LEFT - context.marginRight()) / 10));
		styler.setYAxisTickMarkSpacingHint(Math.max(
				1,
				(context.height() - PaneLayoutCalculator.MARGIN_TOP - PaneLayoutCalculator.MARGIN_BOTTOM) / 10));
		if (context.multiplePanes()) {
			styler.setYAxisLeftWidthHint(PaneLayoutCalculator.MARGIN_LEFT);
		}
		if (!context.lastPane()) {
			styler.setXAxisTitleVisible(false);
			styler.setXAxisTicksVisible(false);
		}
	}
}
