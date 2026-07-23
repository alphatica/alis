package com.alphatica.alis.charting;

import org.knowm.xchart.BitmapEncoder;
import org.knowm.xchart.XYChart;
import org.knowm.xchart.XYChartBuilder;
import org.knowm.xchart.XYSeries;
import org.knowm.xchart.style.Styler;
import org.knowm.xchart.style.XYStyler;
import org.knowm.xchart.style.lines.SeriesLines;
import org.knowm.xchart.style.markers.SeriesMarkers;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static java.util.Objects.requireNonNull;

public class Chart<X extends Comparable<X>> {

	private final List<ChartPane<X>> panes = new ArrayList<>();
	private String copyright;
	private String xName;
	private int marginRight = 300;

	public void addPane(
			Scale scale,
			String title,
			List<LineChartData<X>> lines,
			PaneSettings settings) {
		panes.add(new ChartPane<>(
				requireNonNull(scale, "scale"),
				title,
				List.copyOf(requireNonNull(lines, "lines")),
				requireNonNull(settings, "settings")));
	}

	public void setXName(String xName) {
		this.xName = xName;
	}

	public void setCopyright(String copyright) {
		this.copyright = copyright;
	}

	public void setMarginRight(int marginRight) {
		this.marginRight = marginRight;
	}

	public void createImage(File file) throws IOException {
		ChartSettings settings = new ChartSettings(copyright, xName, marginRight);
		ChartModel<X> model = new ChartModel<>(List.copyOf(panes), settings);
		new WholeChartRenderer<>(model).createImage(file);
	}

	private record ChartPane<X extends Comparable<X>>(
			Scale scale,
			String title,
			List<LineChartData<X>> lines,
			PaneSettings settings) {
	}

	private record ChartSettings(
			String copyright,
			String xName,
			int marginRight) {
	}

	private record ChartModel<X extends Comparable<X>>(
			List<ChartPane<X>> panes,
			ChartSettings settings) {
	}

	private record XAxisLayout<X extends Comparable<X>>(
			List<X> labels,
			Map<X, Double> positions) {
	}

	private static final class WholeChartRenderer<X extends Comparable<X>> {

		private static final int WIDTH = 3840;
		private static final int HEIGHT = 2160;
		private static final int FOOTER_HEIGHT = 100;
		private static final int PANES_HEIGHT = HEIGHT - FOOTER_HEIGHT;
		private static final int FOOTER_BASELINE_OFFSET = 25;
		private static final int MARGIN_LEFT = 250;
		private static final Font COPYRIGHT_FONT = new Font("Helvetica", Font.PLAIN, 25);

		private final ChartModel<X> model;
		private final ChartSettings settings;

		private WholeChartRenderer(ChartModel<X> model) {
			this.model = model;
			settings = model.settings();
		}

		private void createImage(File file) throws IOException {
			List<ChartPane<X>> renderedPanes = panesToRender();
			XAxisLayout<X> xAxisLayout = createXAxisLayout(renderedPanes);
			List<Integer> paneHeights = PaneHeightCalculator.calculate(
					renderedPanes.stream()
							.map(pane -> pane.settings().heightWeight())
							.toList(),
					PANES_HEIGHT);
			double plotContentSize = paneHeights.stream()
					.mapToDouble(height -> PaneRenderer.calculatePlotContentSize(
							height, settings.marginRight()))
					.max()
					.orElseThrow();
			boolean multiplePanes = renderedPanes.size() > 1;
			List<Double> rightReservations = renderedPanes.stream()
					.map(PaneRenderer::calculateRightReservation)
					.toList();
			double largestRightReservation = rightReservations.stream()
					.mapToDouble(Double::doubleValue)
					.max()
					.orElseThrow();

			BufferedImage image = createCanvas();
			Graphics2D graphics = image.createGraphics();
			try {
				int y = 0;
				int lastVisiblePane = lastVisiblePane(paneHeights);
				for (int index = 0; index < renderedPanes.size(); index++) {
					int paneHeight = paneHeights.get(index);
					if (paneHeight == 0) {
						continue;
					}
					int paneWidth = WIDTH - (int) Math.round(
							largestRightReservation - rightReservations.get(index));
					boolean lastPane = index == lastVisiblePane;
					BufferedImage paneImage = new PaneRenderer<>(
							renderedPanes.get(index),
							paneWidth,
							paneHeight,
							xAxisLayout,
							lastPane,
							settings.xName(),
							settings.marginRight(),
							plotContentSize,
							multiplePanes)
							.createImage();
					graphics.drawImage(paneImage, 0, y, null);
					y += paneHeight;
				}
			} finally {
				graphics.dispose();
			}

			addCopyright(image);
			if (!ImageIO.write(image, "PNG", file)) {
				throw new IOException("No PNG image writer is available");
			}
		}

		private static int lastVisiblePane(List<Integer> paneHeights) {
			for (int index = paneHeights.size() - 1; index >= 0; index--) {
				if (paneHeights.get(index) > 0) {
					return index;
				}
			}
			throw new IllegalStateException("At least one pane must have a positive height");
		}

		private List<ChartPane<X>> panesToRender() {
			if (!model.panes().isEmpty()) {
				return model.panes();
			}
			return List.of(new ChartPane<>(
					Scale.ARITHMETIC,
					null,
					List.of(),
					PaneSettings.defaults()));
		}

		private static <X extends Comparable<X>> XAxisLayout<X> createXAxisLayout(
				List<ChartPane<X>> panes) {
			Set<X> values = new TreeSet<>();
			panes.stream()
					.map(ChartPane::lines)
					.flatMap(List::stream)
					.map(LineChartData::getData)
					.map(Map::keySet)
					.forEach(values::addAll);
			List<X> labels = List.copyOf(values);
			Map<X, Double> positions = new HashMap<>();
			for (int index = 0; index < labels.size(); index++) {
				positions.put(labels.get(index), (double) index);
			}
			return new XAxisLayout<>(labels, Map.copyOf(positions));
		}

		private static BufferedImage createCanvas() {
			BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = image.createGraphics();
			try {
				graphics.setColor(Color.BLACK);
				graphics.fillRect(0, 0, WIDTH, HEIGHT);
			} finally {
				graphics.dispose();
			}
			return image;
		}

		private void addCopyright(BufferedImage image) {
			if (settings.copyright() == null) {
				return;
			}
			Graphics2D graphics = image.createGraphics();
			try {
				graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				graphics.setColor(Color.WHITE);
				graphics.setFont(COPYRIGHT_FONT);
				graphics.drawString("© " + settings.copyright(), MARGIN_LEFT, HEIGHT - FOOTER_BASELINE_OFFSET);
			} finally {
				graphics.dispose();
			}
		}
	}

	private static final class PaneRenderer<X extends Comparable<X>> {

		private static final int WIDTH = 3840;
		private static final int MARGIN_TOP = 200;
		private static final int MARGIN_BOTTOM = 150;
		private static final int MARGIN_LEFT = 250;
		private static final int POINT_SIZE = 4;
		private static final int CHART_PADDING = 10;
		private static final int LEGEND_PADDING = 10;
		private static final int LEGEND_SERIES_LINE_LENGTH = 24;
		private static final Font LABEL_FONT = new Font("Monospaced", Font.PLAIN, 30);
		private static final Font TITLE_FONT = new Font("Helvetica", Font.ITALIC, 70);
		private static final Color[] SERIES_COLORS = {
				Color.WHITE, Color.GREEN, Color.ORANGE, Color.CYAN, Color.GRAY, Color.RED,
				Color.MAGENTA, Color.PINK, Color.YELLOW, Color.BLUE, Color.RED
		};

		private final ChartPane<X> pane;
		private final int width;
		private final int height;
		private final XAxisLayout<X> xAxisLayout;
		private final boolean lastPane;
		private final String xAxisTitle;
		private final int marginRight;
		private final double plotContentSize;
		private final boolean multiplePanes;

		private PaneRenderer(
				ChartPane<X> pane,
				int width,
				int height,
				XAxisLayout<X> xAxisLayout,
				boolean lastPane,
				String xAxisTitle,
				int marginRight,
				double plotContentSize,
				boolean multiplePanes) {
			this.pane = pane;
			this.width = width;
			this.height = height;
			this.xAxisLayout = xAxisLayout;
			this.lastPane = lastPane;
			this.xAxisTitle = xAxisTitle;
			this.marginRight = marginRight;
			this.plotContentSize = plotContentSize;
			this.multiplePanes = multiplePanes;
		}

		private BufferedImage createImage() {
			return BitmapEncoder.getBufferedImage(createXChart());
		}

		private XYChart createXChart() {
			XYChart chart = new XYChartBuilder()
					.width(width)
					.height(height)
					.title(pane.title() == null ? "" : pane.title())
					.xAxisTitle(lastPane && xAxisTitle != null ? xAxisTitle : "")
					.yAxisTitle(pane.settings().yAxisTitle() == null ? "" : pane.settings().yAxisTitle())
					.build();

			configureStyle(chart);
			addDataSeries(chart);
			addHorizontalSeries(chart, xRange());
			addEmptyChartAnchor(chart);
			configureAxes(chart);
			chart.getStyler().setLegendVisible(
					chart.getSeriesCollection().stream().anyMatch(XYSeries::isShowInLegend));
			return chart;
		}

		private void configureStyle(XYChart chart) {
			XYStyler styler = chart.getStyler();
			styler.setAntiAlias(true);
			styler.setTextAntiAlias(true);
			styler.setChartBackgroundColor(Color.BLACK);
			styler.setPlotBackgroundColor(Color.BLACK);
			styler.setChartFontColor(Color.LIGHT_GRAY);
			styler.setChartTitleFontColor(Color.WHITE);
			styler.setChartTitleFont(TITLE_FONT);
			styler.setChartTitleVisible(pane.title() != null);
			styler.setAxisTitleFont(LABEL_FONT);
			styler.setAxisTickLabelsFont(LABEL_FONT);
			styler.setAxisTickLabelsColor(Color.LIGHT_GRAY);
			styler.setAxisTickMarksColor(Color.LIGHT_GRAY);
			styler.setXAxisTitleColor(Color.LIGHT_GRAY);
			styler.setYAxisTitleColor(Color.LIGHT_GRAY);
			styler.setPlotBorderVisible(true);
			styler.setPlotBorderColor(Color.LIGHT_GRAY);
			styler.setPlotGridLinesVisible(true);
			styler.setPlotGridLinesColor(Color.DARK_GRAY);
			styler.setSeriesColors(SERIES_COLORS);
			styler.setMarkerSize(POINT_SIZE);
			styler.setLegendPosition(Styler.LegendPosition.OutsideE);
			styler.setLegendBackgroundColor(Color.BLACK);
			styler.setLegendBorderColor(Color.LIGHT_GRAY);
			styler.setLegendFont(LABEL_FONT);
			styler.setPlotContentSize(plotContentSize);
			styler.setXAxisMaxLabelCount(Math.clamp(xAxisLayout.labels().size(), 1, 11));
			styler.setXAxisTickMarkSpacingHint(Math.max(1, (WIDTH - MARGIN_LEFT - marginRight) / 10));
			styler.setYAxisTickMarkSpacingHint(Math.max(1, (height - MARGIN_TOP - MARGIN_BOTTOM) / 10));
			if (multiplePanes) {
				styler.setYAxisLeftWidthHint(MARGIN_LEFT);
			}
			if (!lastPane) {
				styler.setXAxisTitleVisible(false);
				styler.setXAxisTicksVisible(false);
			}
		}

		private static double calculatePlotContentSize(int height, int marginRight) {
			double horizontalSize = (double) (WIDTH - MARGIN_LEFT - marginRight) / WIDTH;
			double verticalSize = (double) (height - MARGIN_TOP - MARGIN_BOTTOM) / height;
			double upperBound = Math.max(0.01, Math.min(1.0, verticalSize));
			double lowerBound = Math.min(0.5, upperBound);
			return Math.clamp(horizontalSize, lowerBound, upperBound);
		}

		private static double calculateRightReservation(ChartPane<?> pane) {
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
			for (String line : label.split("\\n")) {
				if (!line.isEmpty()) {
					TextLayout textLayout = new TextLayout(line, LABEL_FONT, fontRenderContext);
					widestLine = Math.max(widestLine, textLayout.getOutline(null).getBounds2D().getWidth());
				}
			}
			return widestLine;
		}

		private void addDataSeries(XYChart chart) {
			for (int index = 0; index < pane.lines().size(); index++) {
				LineChartData<X> dataLine = pane.lines().get(index);
				List<Double> xData = new ArrayList<>();
				List<Double> yData = new ArrayList<>();
				dataLine.getData().forEach((x, values) -> addPoints(
						xAxisLayout.positions().get(x), values, xData, yData));
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
					yData.add(transformY(value));
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
			List<HorizontalLine> horizontalLines = pane.settings().horizontalLines();
			for (int index = 0; index < horizontalLines.size(); index++) {
				HorizontalLine horizontalLine = horizontalLines.get(index);
				if (!Double.isFinite(horizontalLine.value())) {
					continue;
				}
				XYSeries series = chart.addSeries(
						"horizontal-" + index,
						List.of(range.min(), range.max()),
						List.of(transformY(horizontalLine.value()), transformY(horizontalLine.value())));
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

		private void configureAxes(XYChart chart) {
			XRange range = xRange();
			chart.getStyler().setXAxisMin(range.min());
			chart.getStyler().setXAxisMax(range.max());
			chart.setCustomXAxisTickLabelsFormatter(value -> formatXLabel(value, xAxisLayout.labels()));
			chart.setCustomYAxisTickLabelsFormatter(value -> formatYLabel(inverseTransformY(value)));
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

		private double transformY(double value) {
			if (pane.scale() == Scale.ARITHMETIC) {
				return value;
			}
			return Math.copySign(Math.log1p(Math.abs(value)), value);
		}

		private double inverseTransformY(double value) {
			if (pane.scale() == Scale.ARITHMETIC) {
				return value;
			}
			return Math.copySign(Math.expm1(Math.abs(value)), value);
		}

		private XRange xRange() {
			if (xAxisLayout.labels().size() == 1) {
				return new XRange(-0.5, 0.5);
			}
			return new XRange(0.0, Math.max(1.0, xAxisLayout.labels().size() - 1.0));
		}

		private record XRange(double min, double max) {
		}
	}
}
