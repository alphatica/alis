package com.alphatica.alis.charting;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

final class WholeChartRenderer<X extends Comparable<X>> {

	private static final int WIDTH = 3840;
	private static final int HEIGHT = 2160;
	private static final int FOOTER_HEIGHT = 100;
	private static final int PANES_HEIGHT = HEIGHT - FOOTER_HEIGHT;
	private static final int FOOTER_BASELINE_OFFSET = 25;
	private static final int MARGIN_LEFT = 250;
	private static final Font COPYRIGHT_FONT = new Font("Helvetica", Font.PLAIN, 25);

	private final ChartModel<X> model;
	private final ChartSettings settings;

	WholeChartRenderer(ChartModel<X> model) {
		this.model = model;
		settings = model.settings();
	}

	void createImage(File file) throws IOException {
		List<ChartPane<X>> renderedPanes = panesToRender();
		XAxisLayout<X> xAxisLayout = createXAxisLayout(renderedPanes);
		List<Integer> paneHeights = PaneHeightCalculator.calculate(
				renderedPanes.stream()
						.map(pane -> pane.settings().heightWeight())
						.toList(),
				PANES_HEIGHT);
		double plotContentSize = paneHeights.stream()
				.mapToDouble(height -> PaneLayoutCalculator.calculatePlotContentSize(
						height, settings.marginRight()))
				.max()
				.orElseThrow();
		boolean multiplePanes = renderedPanes.size() > 1;
		List<Double> rightReservations = renderedPanes.stream()
				.map(PaneLayoutCalculator::calculateRightReservation)
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
				PaneRenderContext<X> context = new PaneRenderContext<>(
						renderedPanes.get(index),
						paneWidth,
						paneHeight,
						xAxisLayout,
						lastPane,
						settings.xName(),
						settings.marginRight(),
						plotContentSize,
						multiplePanes);
				BufferedImage paneImage = new PaneRenderer<>(context)
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
