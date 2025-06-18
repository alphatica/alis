package com.alphatica.alis.tools.data;

public class FloatArraySlice {
	private final float[] array;
	private final int start;

	public FloatArraySlice(float[] array, int start) {
		this.array = array;
		this.start = start;
	}

	public int size() {
		return array.length - start;
	}

	public float get(int i) {
		if (i < 0 || i >= size()) {
			throw new IndexOutOfBoundsException("Index: " + i + ", Size: " + size());
		}
		return array[start + i];
	}
}
