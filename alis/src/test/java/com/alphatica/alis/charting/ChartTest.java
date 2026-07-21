package com.alphatica.alis.charting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChartTest {

	private static final int EXPECTED_WIDTH = 3840;
	private static final int EXPECTED_HEIGHT = 2160;

	@TempDir
	Path tempDirectory;

	@Test
	void shouldCreateChartWithAllSupportedElements() throws IOException {
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
		chart.addDataLines(List.of(connectedLine, points));
		chart.addHorizontalLine(new HorizontalLine("Average", 20.0));
		chart.setTitle("XChart adapter test");
		chart.setXName("Date");
		chart.setYName("Change");
		chart.setCopyright("Alphatica.com");
		chart.setLogarithmic(true);

		assertEquals(List.of("2026-01-01", "2026-01-02", "2026-01-03"), chart.xValues());
		assertPng(chart, "chart.png");
	}

	@Test
	void shouldCreateEmptyChart() throws IOException {
		assertPng(new Chart<>(), "empty-chart.png");
	}

	private void assertPng(Chart<?> chart, String fileName) throws IOException {
		File file = tempDirectory.resolve(fileName).toFile();
		chart.createImage(file);

		BufferedImage image = ImageIO.read(file);
		assertNotNull(image);
		assertEquals(EXPECTED_WIDTH, image.getWidth());
		assertEquals(EXPECTED_HEIGHT, image.getHeight());
		assertTrue(Files.size(file.toPath()) > 10_000);
	}
}
