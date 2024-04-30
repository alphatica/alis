package com.alphatica.alis.tools.data;

public class DoubleArraySlice {
	private final double[] array;
	private final int start;

	public DoubleArraySlice(double[] array, int start) {
		this.array = array;
		this.start = start;
	}

	public int size() {
		return array.length - start;
	}

	public double get(int i) {
		if (i < 0 || i >= size()) {
			throw new IndexOutOfBoundsException("Index: " + i + ", Size: " + size());
		}
		return array[start + i];
	}
}
