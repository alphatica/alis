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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Chart<X extends Comparable<X>> {

	private final List<LineChartData<X>> dataLines = new ArrayList<>();
	private final List<HorizontalLine> horizontalLines = new ArrayList<>();
	private String copyright;
	private String title;
	private String xName;
	private String yName;
	private boolean isLogarithmic;
	private int marginRight = 300;

	public void setLogarithmic(boolean logarithmic) {
		isLogarithmic = logarithmic;
	}

	public void setXName(String xName) {
		this.xName = xName;
	}

	public void setYName(String yName) {
		this.yName = yName;
	}

	public void setCopyright(String copyright) {
		this.copyright = copyright;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void addDataLines(List<LineChartData<X>> dataLines) {
		this.dataLines.addAll(dataLines);
	}

	public void addHorizontalLine(HorizontalLine horizontalLine) {
		horizontalLines.add(horizontalLine);
	}

	public void setMarginRight(int marginRight) {
		this.marginRight = marginRight;
	}

	public void createImage(File file) throws IOException {
		ChartSettings settings = new ChartSettings(copyright, title, xName, yName, isLogarithmic, marginRight);
		ChartModel<X> model = new ChartModel<>(List.copyOf(dataLines), List.copyOf(horizontalLines), xValues(), settings);
		new XChartRenderer<>(model).createImage(file);
	}

	List<X> xValues() {
		Set<X> values = dataLines.stream()
				.map(line -> line.getData().keySet())
				.flatMap(Set::stream)
				.collect(Collectors.toSet());
		return values.stream().sorted().toList();
	}

	private record ChartSettings(
			String copyright,
			String title,
			String xName,
			String yName,
			boolean logarithmic,
			int marginRight) {
	}

	private record ChartModel<X extends Comparable<X>>(
			List<LineChartData<X>> dataLines,
			List<HorizontalLine> horizontalLines,
			List<X> xValues,
			ChartSettings settings) {
	}

	private static final class XChartRenderer<X extends Comparable<X>> {

		private static final int WIDTH = 3840;
		private static final int HEIGHT = 2160;
		private static final int FOOTER_HEIGHT = 100;
		private static final int CHART_HEIGHT = HEIGHT - FOOTER_HEIGHT;
		private static final int FOOTER_BASELINE_OFFSET = 25;
		private static final int MARGIN_TOP = 200;
		private static final int MARGIN_BOTTOM = 150;
		private static final int MARGIN_LEFT = 250;
		private static final int POINT_SIZE = 4;
		private static final Font LABEL_FONT = new Font("Monospaced", Font.PLAIN, 30);
		private static final Font TITLE_FONT = new Font("Helvetica", Font.ITALIC, 70);
		private static final Font COPYRIGHT_FONT = new Font("Helvetica", Font.PLAIN, 25);
		private static final Color[] SERIES_COLORS = {
				Color.WHITE, Color.GREEN, Color.ORANGE, Color.CYAN, Color.GRAY, Color.RED,
				Color.MAGENTA, Color.PINK, Color.YELLOW, Color.BLUE, Color.RED
		};

		private final ChartModel<X> model;
		private final ChartSettings settings;

		private XChartRenderer(ChartModel<X> model) {
			this.model = model;
			settings = model.settings();
		}

		private void createImage(File file) throws IOException {
			BufferedImage chartImage = BitmapEncoder.getBufferedImage(createXChart());
			BufferedImage image = addFooterSpace(chartImage);
			addCopyright(image);
			if (!ImageIO.write(image, "PNG", file)) {
				throw new IOException("No PNG image writer is available");
			}
		}

		private XYChart createXChart() {
			XYChart chart = new XYChartBuilder()
					.width(WIDTH)
					.height(CHART_HEIGHT)
					.title(settings.title() == null ? "" : settings.title())
					.xAxisTitle(settings.xName() == null ? "" : settings.xName())
					.yAxisTitle(settings.yName() == null ? "" : settings.yName())
					.build();

			configureStyle(chart);
			Map<X, Double> xPositions = createXPositions();
			addDataSeries(chart, xPositions);
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
			styler.setChartTitleVisible(settings.title() != null);
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
			styler.setPlotContentSize(plotContentSize());
			styler.setXAxisMaxLabelCount(Math.clamp(model.xValues().size(), 1, 11));
			styler.setXAxisTickMarkSpacingHint(Math.max(1, (WIDTH - MARGIN_LEFT - settings.marginRight()) / 10));
			styler.setYAxisTickMarkSpacingHint(Math.max(1, (CHART_HEIGHT - MARGIN_TOP - MARGIN_BOTTOM) / 10));
		}

		private double plotContentSize() {
			double horizontalSize = (double) (WIDTH - MARGIN_LEFT - settings.marginRight()) / WIDTH;
			double verticalSize = (double) (CHART_HEIGHT - MARGIN_TOP - MARGIN_BOTTOM) / CHART_HEIGHT;
			return Math.clamp(horizontalSize, 0.5, verticalSize);
		}

		private Map<X, Double> createXPositions() {
			Map<X, Double> positions = new HashMap<>();
			for (int index = 0; index < model.xValues().size(); index++) {
				positions.put(model.xValues().get(index), (double) index);
			}
			return positions;
		}

		private void addDataSeries(XYChart chart, Map<X, Double> xPositions) {
			for (int index = 0; index < model.dataLines().size(); index++) {
				LineChartData<X> dataLine = model.dataLines().get(index);
				List<Double> xData = new ArrayList<>();
				List<Double> yData = new ArrayList<>();
				dataLine.getData().forEach((x, values) -> addPoints(xPositions.get(x), values, xData, yData));
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

		private static <X extends Comparable<X>> void configureDataSeries(XYSeries series, LineChartData<X> dataLine) {
			series.setMarker(SeriesMarkers.SQUARE);
			series.setXYSeriesRenderStyle(dataLine.isConnectPoints()
					? XYSeries.XYSeriesRenderStyle.Line
					: XYSeries.XYSeriesRenderStyle.Scatter);
			series.setLineStyle(dataLine.isConnectPoints() ? SeriesLines.SOLID : SeriesLines.NONE);
			series.setLineWidth(3.0f);
			configureLegendEntry(series, dataLine.getName());
		}

		private void addHorizontalSeries(XYChart chart, XRange range) {
			for (int index = 0; index < model.horizontalLines().size(); index++) {
				HorizontalLine horizontalLine = model.horizontalLines().get(index);
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
			chart.setCustomXAxisTickLabelsFormatter(value -> formatXLabel(value, model.xValues()));
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
			if (!settings.logarithmic()) {
				return value;
			}
			// Financial changes can be zero or negative, which XChart's native logarithmic axis rejects.
			return Math.copySign(Math.log1p(Math.abs(value)), value);
		}

		private double inverseTransformY(double value) {
			if (!settings.logarithmic()) {
				return value;
			}
			return Math.copySign(Math.expm1(Math.abs(value)), value);
		}

		private XRange xRange() {
			if (model.xValues().size() == 1) {
				return new XRange(-0.5, 0.5);
			}
			return new XRange(0.0, Math.max(1.0, model.xValues().size() - 1.0));
		}

		private static BufferedImage addFooterSpace(BufferedImage chartImage) {
			BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
			Graphics2D graphics = image.createGraphics();
			try {
				graphics.setColor(Color.BLACK);
				graphics.fillRect(0, 0, WIDTH, HEIGHT);
				graphics.drawImage(chartImage, 0, 0, null);
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

		private record XRange(double min, double max) {
		}
	}
}
