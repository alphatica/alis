package com.alphatica.alis.tools.data;

public class DoubleArrayRange {
    private final double[] array;
    private final int start;
    private final int end;

    public DoubleArrayRange(double[] array, int start, int end) {
        this.array = array;
        this.start = start;
        this.end = end;
    }

    public int size() {
        return end - start;
    }

    public double get(int i) {
        if (i < 0 || i >= size()) {
            throw new IndexOutOfBoundsException("Index: " + i + ", Size: " + size());
        }
        return array[start + i];
    }
}
