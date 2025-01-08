package com.alphatica.alis.charting;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

import static com.alphatica.alis.tools.java.CollectionsTools.arrayList;

public class Chart<X extends Comparable<X>> {

	private static final Font CHART_LABELS_FONT = new Font("Monospaced", Font.PLAIN, 30);
	private static final int MARGIN_TOP = 200;
	private static final int MARGIN_BOTTOM = 150;
	private static final int MARGIN_LEFT = 250;
	private static final int MARGIN_RIGHT = 300;
	private static final int WIDTH = 3840;
	private static final int HEIGHT = 2160;
	private static final int POINT_SIZE = 4;
	private static final int Y_AXIS_LABEL_OFFSET = 15;
	private static final int HORIZONTAL_GRID_LABEL_OFFSET = 170;

	private final List<LineChartData<X>> dataLines = new ArrayList<>();
	private final List<HorizontalLine> horizontalLines = new ArrayList<>();
	private String copyright = null;
	private String title = null;
	private String xName = null;
	private String yName = null;
	private boolean isLogarithmic = false;

	private static int drawLineLabel(String line, Graphics2D graphics, int offset) {
		if (line != null) {
			graphics.drawString(line, WIDTH - MARGIN_RIGHT + 20, offset);
			offset += 50;
		}
		return offset;
	}

	private static <X> void drawVerticalGrid(Graphics2D graphics, List<X> xValues, int gridLines) {
		double xStep = (double) (xValues.size() - 1) / gridLines;
		int xSpace = (WIDTH - MARGIN_RIGHT - MARGIN_LEFT) / gridLines;
		for (int i = 0; i <= gridLines; i++) {
			X x = xValues.get((int) Math.round(i * xStep));
			int xPix = i * xSpace + MARGIN_LEFT;
			graphics.drawLine(xPix, MARGIN_TOP, xPix, HEIGHT - MARGIN_BOTTOM);
			graphics.drawString(x.toString(), xPix, HEIGHT - MARGIN_BOTTOM + 35);
		}
	}

	private static String getYAxisLabel(double y) {
		return String.format("%6.0f", y);
	}

	private static void drawPointConnection(Graphics2D graphics, int lastX, int lastY, int x, int y) {
		for (int i = 0; i < POINT_SIZE; i++) {
			graphics.drawLine(lastX + i, lastY + i, x + i, y + 1);
		}
	}

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
		this.horizontalLines.add(horizontalLine);
	}

	public void createImage(File file) throws IOException {
		BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		initCanvas(graphics);

		MinMaxValue yMinMax = new MinMaxValue(dataLines);
		List<X> xValues = xValues();
		drawAxes(graphics, yMinMax, xValues);
		drawLines(graphics, xValues, yMinMax);
		addCopyright(graphics);
		addTitle(graphics);

		ImageIO.write(image, "PNG", file);
		graphics.dispose();
	}

	private void drawLines(Graphics2D graphics, List<X> xValues, MinMaxValue yMinMax) {
		int offset = MARGIN_TOP + 20;
		List<Color> colors = arrayList(Color.WHITE, Color.GREEN, Color.ORANGE, Color.CYAN, Color.GRAY, Color.RED, Color.MAGENTA, Color.PINK,
				Color.YELLOW, Color.BLUE, Color.RED);
		graphics.setFont(CHART_LABELS_FONT);
		offset = drawDataLines(graphics, xValues, yMinMax, colors, offset);
		drawHorizontalLines(graphics, yMinMax, colors, offset);
	}

	private int drawDataLines(Graphics2D graphics, List<X> xValues, MinMaxValue yMinMax, List<Color> colors, int offset) {
		for (LineChartData<X> line : dataLines) {
			setNextColor(graphics, colors);
			offset = drawLineLabel(line.getName(), graphics, offset);
			int lastX = 0;
			int lastY = 0;
			boolean firstPoint = true;
			SortedMap<X, List<Double>> data = line.getData();
			for (Map.Entry<X, List<Double>> entry : data.entrySet()) {
				int x = xToPix(entry.getKey(), xValues);
				for (Double value : entry.getValue()) {
					int y = yToPix(value, yMinMax);
					graphics.fillRect(x, y, POINT_SIZE, POINT_SIZE);
					if (line.isConnectPoints()) {
						if (!firstPoint) {
							drawPointConnection(graphics, lastX, lastY, x, y);
						}
						firstPoint = false;
						lastX = x;
						lastY = y;
					}
				}
			}
		}
		return offset;
	}

	private void drawHorizontalLines(Graphics2D graphics, MinMaxValue yMinMax, List<Color> colors, int offset) {
		for (HorizontalLine line : horizontalLines) {
			setNextColor(graphics, colors);
			offset = drawLineLabel(line.name(), graphics, offset);
			int y = yToPix(line.value(), yMinMax);
			graphics.drawLine(MARGIN_LEFT, y, WIDTH - MARGIN_RIGHT, y);
			String label = getYAxisLabel(line.value());
			graphics.drawString(label, MARGIN_LEFT - HORIZONTAL_GRID_LABEL_OFFSET, y + Y_AXIS_LABEL_OFFSET);
		}
	}

	private void setNextColor(Graphics2D graphics, List<Color> colors) {
		if (!colors.isEmpty()) {
			graphics.setColor(colors.removeFirst());
		}
	}

	private void initCanvas(Graphics2D graphics) {
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		graphics.setColor(Color.BLACK);
		graphics.fillRect(0, 0, WIDTH, HEIGHT);
	}

	private void addTitle(Graphics2D graphics) {
		if (title != null) {
			graphics.setColor(Color.WHITE);
			graphics.setFont(new Font("Helvetica", Font.ITALIC, 70));
			int x = WIDTH / 2 - 40 * (title.length() / 2);
			graphics.drawString(title, x, MARGIN_TOP - 80);
		}
	}

	private void addCopyright(Graphics2D graphics) {
		if (copyright != null) {
			graphics.setColor(Color.WHITE);
			graphics.setFont(new Font("Helvetica", Font.PLAIN, 25));
			graphics.drawString("© " + copyright, MARGIN_LEFT, HEIGHT - 60);
		}
	}

	private void drawAxes(Graphics2D graphics, MinMaxValue yMinMax, List<X> xValues) {
		graphics.setColor(Color.LIGHT_GRAY);
		graphics.setFont(CHART_LABELS_FONT);
		int xAxisY;
		if (0 > yMinMax.getMin() && 0 < yMinMax.getMax()) {
			xAxisY = yToPix(0, yMinMax);
			graphics.drawString(getYAxisLabel(0), MARGIN_LEFT - HORIZONTAL_GRID_LABEL_OFFSET, xAxisY + Y_AXIS_LABEL_OFFSET);
		} else {
			xAxisY = yToPix(yMinMax.getMin(), yMinMax);
			graphics.drawString(getYAxisLabel(yMinMax.getMin()), MARGIN_LEFT - HORIZONTAL_GRID_LABEL_OFFSET, xAxisY + Y_AXIS_LABEL_OFFSET);
		}
		graphics.fillRect(MARGIN_LEFT, xAxisY, WIDTH - MARGIN_RIGHT - MARGIN_LEFT, 2);
		graphics.fillRect(MARGIN_LEFT, MARGIN_TOP, 2, HEIGHT - MARGIN_BOTTOM - MARGIN_TOP);
		if (xName != null) {
			graphics.drawString(xName, WIDTH - MARGIN_RIGHT + 10, xAxisY);
		}
		if (yName != null) {
			graphics.drawString(yName, MARGIN_LEFT, MARGIN_TOP - 10);
		}
		int gridLines = 10;
		drawHorizontalGrid(graphics, yMinMax, gridLines);
		drawVerticalGrid(graphics, xValues, gridLines);
	}

	private void drawHorizontalGrid(Graphics2D graphics, MinMaxValue yMinMax, int gridLines) {
		double yGridStep = (yMinMax.getMax() - yMinMax.getMin()) / (gridLines - 1);
		for (int i = 0; i < gridLines; i++) {
			double y = i * yGridStep + yMinMax.getMin();
			int yPix = yToPix(y, yMinMax);
			graphics.drawLine(MARGIN_LEFT, yPix, WIDTH - MARGIN_RIGHT, yPix);
			String label = getYAxisLabel(y);
			graphics.drawString(label, MARGIN_LEFT - HORIZONTAL_GRID_LABEL_OFFSET, yPix + Y_AXIS_LABEL_OFFSET);
		}
	}

	private int xToPix(X key, List<X> xValues) {
		int canvas = WIDTH - MARGIN_LEFT - MARGIN_RIGHT;
		double pixPerX = (double) canvas / (double) xValues.size();
		int xIndex = Collections.binarySearch(xValues, key);
		return (int) Math.round(MARGIN_LEFT + xIndex * pixPerX);
	}

	private int yToPix(double y, MinMaxValue yMinMax) {
		if (isLogarithmic) {
			return HEIGHT - MARGIN_BOTTOM - getLogYPix(y, yMinMax);
		} else {
			return HEIGHT - MARGIN_BOTTOM - getArithmeticYPix(y, yMinMax);
		}
	}

	private int getArithmeticYPix(double y, MinMaxValue yMinMax) {
		int canvas = HEIGHT - MARGIN_TOP - MARGIN_BOTTOM;
		double pixPerY = canvas / (yMinMax.getMax() - yMinMax.getMin());
		double yOffset = y - yMinMax.getMin();
		double yOffsetPix = pixPerY * yOffset;
		return (int) Math.round(yOffsetPix);
	}

	private int getLogYPix(double y, MinMaxValue yMinMax) {
		double adjustedMax = yMinMax.getMax();
		double adjustedMin = yMinMax.getMin();
		double adjustedY = y;
		while (adjustedY - adjustedMin < 1.0) {
			adjustedMax *= 10;
			adjustedMin *= 10;
			adjustedY *= 10;
		}
		double yRangeLn = Math.log(adjustedMax - adjustedMin);
		double yLn = Math.log(adjustedY - adjustedMin);
		int canvas = HEIGHT - MARGIN_TOP - MARGIN_BOTTOM;
		return (int) Math.round(canvas * yLn / yRangeLn);
	}

	List<X> xValues() {
		Set<X> set = dataLines.stream().map(line -> line.getData().keySet()).flatMap(Set::stream).collect(Collectors.toSet());
		return set.stream().sorted().toList();
	}
}
