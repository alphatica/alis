package com.alphatica.alis.tools.java;

import static java.lang.String.format;

public class NumberTools {
	private NumberTools() {
	}

	public static void assertPositive(double... values) {
		for (int i = 0; i < values.length; i++) {
			if (values[i] <= 0.0) {
				throw new IllegalArgumentException(format("Value %f at index %d is negative", values[i], i));
			}
		}
	}

	public static double percentChange(double initial, double now) {
		return ((now / initial) - 1.0) * 100;
	}
}
