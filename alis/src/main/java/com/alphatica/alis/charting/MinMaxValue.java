package com.alphatica.alis.charting;

import java.util.List;

public class MinMaxValue {
	private double min = Double.MAX_VALUE;
	private double max = Double.MIN_VALUE;

	public <X extends Comparable<X>> MinMaxValue(List<LineChartData<X>> lines) {
		for (LineChartData<X> line : lines) {
			for (List<Double> data : line.getData().values()) {
				for (Double d : data) {
					if (d < min) {
						min = d;
					} else if (d > max) {
						max = d;
					}
				}
			}
		}
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}
}
