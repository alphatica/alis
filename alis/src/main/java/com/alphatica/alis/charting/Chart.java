package com.alphatica.alis.charting;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
}
