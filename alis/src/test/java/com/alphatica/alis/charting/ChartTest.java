package com.alphatica.alis.charting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartTest {

	private static final int EXPECTED_WIDTH = 3840;
	private static final int EXPECTED_HEIGHT = 2160;
	private static final int PANES_HEIGHT = 2060;

	@TempDir
	Path tempDirectory;

	@Test
	void shouldCreateSinglePaneChartWithAllSupportedElements() throws IOException {
		LineChartData<String> connectedLine = new LineChartData<>("Connected");
		connectedLine.setConnectPoints(true);
		connectedLine.addPoint("2026-01-03", 100.0);
		connectedLine.addPoint("2026-01-01", -50.0);
		connectedLine.addPoint("2026-01-02", 0.0);
		connectedLine.addPoint("2026-01-02", 25.0);

		LineChartData<String> points = new LineChartData<>("Points");
		points.addPoint("2026-01-01", 10.0);
		points.addPoint("2026-01-03", 75.0);

		Chart<String> chart = new Chart<>();
		chart.addPane(
				Scale.LOGARITHMIC,
				"XChart adapter test",
				List.of(connectedLine, points),
				new PaneSettings(
						1.0,
						"Change",
						List.of(new HorizontalLine("Average", 20.0))));
		chart.setXName("Date");
		chart.setCopyright("Alphatica.com");

		assertPng(chart, "chart.png");
	}

	@Test
	void shouldCreateEmptyChart() throws IOException {
		assertPng(new Chart<>(), "empty-chart.png");
	}

	@Test
	void shouldRenderThreePanelsWithIndependentScalesLinesAndXValues() throws IOException {
		LineChartData<String> arithmeticLine = connectedLine("Arithmetic", "A", 10.0, "C", 30.0);
		LineChartData<String> arithmeticPoints = new LineChartData<>("Points");
		arithmeticPoints.addPoint("B", 20.0);

		LineChartData<String> logarithmicLine = connectedLine("Signed log", "B", -100.0, "D", 100.0);
		logarithmicLine.addPoint("C", 0.0);

		Chart<String> chart = new Chart<>();
		chart.addPane(
				Scale.ARITHMETIC,
				"Arithmetic pane",
				List.of(arithmeticLine, arithmeticPoints),
				new PaneSettings(3.0, "Arithmetic value", List.of(new HorizontalLine("First level", 15.0))));
		chart.addPane(
				Scale.LOGARITHMIC,
				"Logarithmic pane",
				List.of(logarithmicLine),
				new PaneSettings(1.0, "Log value", List.of(new HorizontalLine("Second level", -50.0))));
		chart.addPane(Scale.ARITHMETIC, "Empty pane", List.of(), new PaneSettings(2.0, null, List.of()));
		chart.setXName("Shared X");
		chart.setCopyright("Alphatica.com");

		assertPng(chart, "three-panes.png");
	}

	@Test
	void shouldAllocatePanelHeightsFromWeights() {
		assertEquals(List.of(1545, 515), PaneHeightCalculator.calculate(List.of(3.0, 1.0), PANES_HEIGHT));
		assertEquals(List.of(1030, 343, 687), PaneHeightCalculator.calculate(List.of(3.0, 1.0, 2.0), PANES_HEIGHT));
		assertEquals(
				PANES_HEIGHT,
				PaneHeightCalculator.calculate(List.of(7.0, 11.0, 13.0), PANES_HEIGHT)
						.stream()
						.mapToInt(Integer::intValue)
						.sum());
	}

	@Test
	void shouldAllocateHeightsWithoutOverflowForLargeFiniteWeights() {
		assertEquals(
				List.of(1030, 1030),
				PaneHeightCalculator.calculate(List.of(Double.MAX_VALUE, Double.MAX_VALUE), PANES_HEIGHT));
	}

	@ParameterizedTest
	@ValueSource(doubles = {0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
	void shouldRejectInvalidHeightWeight(double weight) {
		assertThrows(IllegalArgumentException.class, () -> new PaneSettings(weight, null, List.of()));
	}

	@Test
	void shouldDefensivelyCopyPaneConfiguration() throws IOException {
		List<HorizontalLine> horizontalLines = new ArrayList<>();
		horizontalLines.add(new HorizontalLine("Original", 1.0));
		PaneSettings settings = new PaneSettings(1.0, null, horizontalLines);
		horizontalLines.add(new HorizontalLine("Added later", 2.0));
		assertEquals(List.of(new HorizontalLine("Original", 1.0)), settings.horizontalLines());
		assertThrows(UnsupportedOperationException.class, () -> settings.horizontalLines().clear());

		List<LineChartData<String>> lines = new ArrayList<>();
		lines.add(connectedLine(null, "A", 1.0, "B", 2.0));
		Chart<String> chart = new Chart<>();
		chart.addPane(Scale.ARITHMETIC, null, lines, settings);
		lines.clear();
		assertPng(chart, "copied-lines.png");
	}

	@Test
	void shouldShowLegendOnlyForNamedSeries() throws IOException {
		BufferedImage unnamedImage = renderSingleLineChart(null, "unnamed.png");
		BufferedImage namedImage = renderSingleLineChart("Visible legend", "named.png");

		long unnamedRightPixels = countNonBlackPixels(unnamedImage, 3200, 0, EXPECTED_WIDTH, PANES_HEIGHT);
		long namedRightPixels = countNonBlackPixels(namedImage, 3200, 0, EXPECTED_WIDTH, PANES_HEIGHT);
		assertTrue(namedRightPixels > unnamedRightPixels);
	}

	@Test
	void shouldRenderXAxisOnlyOnLowestPane() throws IOException {
		LineChartData<String> first = connectedLine(null, "A", 1.0, "B", 2.0);
		LineChartData<String> second = connectedLine(null, "A", 2.0, "B", 1.0);
		Chart<String> chart = new Chart<>();
		chart.addPane(Scale.ARITHMETIC, null, List.of(first), PaneSettings.defaults());
		chart.addPane(Scale.ARITHMETIC, null, List.of(second), PaneSettings.defaults());
		chart.setXName("Bottom axis title");

		BufferedImage image = render(chart, "shared-x-axis.png");
		long upperBottomPixels = countNonBlackPixels(image, 0, 900, 3200, 1030);
		long lowerBottomPixels = countNonBlackPixels(image, 0, 1930, 3200, PANES_HEIGHT);
		assertTrue(lowerBottomPixels > upperBottomPixels);
	}

	@Test
	void shouldAlignSharedXValuesBetweenDifferentHeightPanels() throws IOException {
		LineChartData<String> first = connectedLine("A much wider legend", "A", 1.0, "C", 1.0);
		LineChartData<String> second = connectedLine(null, "A", 1.0, "C", 1.0);
		Chart<String> chart = new Chart<>();
		chart.addPane(Scale.ARITHMETIC, null, List.of(first), new PaneSettings(3.0, null, List.of()));
		chart.addPane(Scale.ARITHMETIC, null, List.of(second), new PaneSettings(1.0, null, List.of()));

		BufferedImage image = render(chart, "aligned-x-values.png");
		assertEquals(longestWhiteRunXRange(image, 0, 1545), longestWhiteRunXRange(image, 1545, PANES_HEIGHT));
	}

	@Test
	void shouldNotExposeLegacyPaneMutationMethods() {
		Set<String> methodNames = Arrays.stream(Chart.class.getDeclaredMethods())
				.map(Method::getName)
				.collect(Collectors.toSet());
		assertTrue(methodNames.contains("addPane"));
		assertFalse(methodNames.contains("addDataLines"));
		assertFalse(methodNames.contains("addHorizontalLine"));
		assertFalse(methodNames.contains("setLogarithmic"));
		assertFalse(methodNames.contains("setTitle"));
		assertFalse(methodNames.contains("setYName"));
		assertFalse(methodNames.contains("xValues"));
	}

	private static LineChartData<String> connectedLine(
			String name,
			String firstX,
			double firstY,
			String secondX,
			double secondY) {
		LineChartData<String> line = new LineChartData<>(name);
		line.setConnectPoints(true);
		line.addPoint(firstX, firstY);
		line.addPoint(secondX, secondY);
		return line;
	}

	private BufferedImage renderSingleLineChart(String name, String fileName) throws IOException {
		Chart<String> chart = new Chart<>();
		chart.addPane(
				Scale.ARITHMETIC,
				null,
				List.of(connectedLine(name, "A", 1.0, "B", 2.0)),
				PaneSettings.defaults());
		return render(chart, fileName);
	}

	private void assertPng(Chart<?> chart, String fileName) throws IOException {
		BufferedImage image = render(chart, fileName);
		assertNotNull(image);
		assertEquals(EXPECTED_WIDTH, image.getWidth());
		assertEquals(EXPECTED_HEIGHT, image.getHeight());
		assertTrue(Files.size(tempDirectory.resolve(fileName)) > 10_000);
	}

	private BufferedImage render(Chart<?> chart, String fileName) throws IOException {
		File file = tempDirectory.resolve(fileName).toFile();
		chart.createImage(file);
		return ImageIO.read(file);
	}

	private static long countNonBlackPixels(
			BufferedImage image,
			int minX,
			int minY,
			int maxX,
			int maxY) {
		long count = 0;
		for (int y = minY; y < maxY; y++) {
			for (int x = minX; x < maxX; x++) {
				if ((image.getRGB(x, y) & 0x00FF_FFFF) != 0) {
					count++;
				}
			}
		}
		return count;
	}

	private static List<Integer> longestWhiteRunXRange(BufferedImage image, int minY, int maxY) {
		int longestStart = -1;
		int longestEnd = -1;
		for (int y = minY; y < maxY; y++) {
			int runStart = -1;
			for (int x = 0; x < EXPECTED_WIDTH; x++) {
				if ((image.getRGB(x, y) & 0x00FF_FFFF) == 0x00FF_FFFF) {
					if (runStart < 0) {
						runStart = x;
					}
				} else if (runStart >= 0) {
					if (x - runStart > longestEnd - longestStart) {
						longestStart = runStart;
						longestEnd = x - 1;
					}
					runStart = -1;
				}
			}
			if (runStart >= 0 && EXPECTED_WIDTH - runStart > longestEnd - longestStart) {
				longestStart = runStart;
				longestEnd = EXPECTED_WIDTH - 1;
			}
		}
		return List.of(longestStart, longestEnd);
	}
}
