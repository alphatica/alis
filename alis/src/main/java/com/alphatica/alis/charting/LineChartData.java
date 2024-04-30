package com.alphatica.alis.charting;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

public class LineChartData<X extends Comparable<X>> {
	private final TreeMap<X, List<Double>> data;
	private String name = null;
	private boolean connectPoints = false;

	public LineChartData() {
		this.data = new TreeMap<>();
	}

	public LineChartData(String name) {
		this.data = new TreeMap<>();
		this.name = name;
	}

	public boolean isConnectPoints() {
		return connectPoints;
	}

	public void setConnectPoints(boolean connectPoints) {
		this.connectPoints = connectPoints;
	}

	public void addPoint(X x, double y) {
		data.computeIfAbsent(x, k -> new ArrayList<>()).add(y);
	}

	public SortedMap<X, List<Double>> getData() {
		return data;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
