package com.alphatica.alis.charting;

import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.util.List;

final class PaneLayoutCalculator {

	static final int WIDTH = 3840;
	static final int MARGIN_TOP = 200;
	static final int MARGIN_BOTTOM = 150;
	static final int MARGIN_LEFT = 250;
	private static final int CHART_PADDING = 10;
	private static final int LEGEND_PADDING = 10;
	private static final int LEGEND_SERIES_LINE_LENGTH = 24;

	private PaneLayoutCalculator() {
	}

	static double calculatePlotContentSize(int height, int marginRight) {
		double horizontalSize = (double) (WIDTH - MARGIN_LEFT - marginRight) / WIDTH;
		double verticalSize = (double) (height - MARGIN_TOP - MARGIN_BOTTOM) / height;
		double upperBound = Math.clamp(verticalSize, 0.01, 1.0);
		double lowerBound = Math.min(0.5, upperBound);
		return Math.clamp(horizontalSize, lowerBound, upperBound);
	}

	static double calculateRightReservation(ChartPane<?> pane) {
		double widestLabel = 0.0;
		for (LineChartData<?> line : pane.lines()) {
			if (line.getName() != null && containsFiniteValue(line)) {
				widestLabel = Math.max(widestLabel, labelWidth(line.getName()));
			}
		}
		for (HorizontalLine line : pane.settings().horizontalLines()) {
			if (line.name() != null && Double.isFinite(line.value())) {
				widestLabel = Math.max(widestLabel, labelWidth(line.name()));
			}
		}
		if (widestLabel == 0.0) {
			return CHART_PADDING;
		}
		double maximumLabelWidth = WIDTH * 0.45;
		double legendWidth = LEGEND_SERIES_LINE_LENGTH
				+ 3.0 * LEGEND_PADDING
				+ Math.min(widestLabel, maximumLabelWidth);
		return legendWidth + 2.0 * CHART_PADDING;
	}

	private static boolean containsFiniteValue(LineChartData<?> line) {
		return line.getData().values().stream()
				.flatMap(List::stream)
				.anyMatch(value -> value != null && Double.isFinite(value));
	}

	private static double labelWidth(String label) {
		FontRenderContext fontRenderContext = new FontRenderContext(null, true, false);
		double widestLine = 0.0;
		for (String line : label.split("\\R")) {
			if (!line.isEmpty()) {
				TextLayout textLayout = new TextLayout(line, PaneTheme.LABEL_FONT, fontRenderContext);
				widestLine = Math.max(widestLine, textLayout.getOutline(null).getBounds2D().getWidth());
			}
		}
		return widestLine;
	}
}
