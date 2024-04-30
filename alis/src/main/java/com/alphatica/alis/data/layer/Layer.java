package com.alphatica.alis.data.layer;

public enum Layer {
	OPEN(0), HIGH(1), LOW(2), CLOSE(3), TURNOVER(4), PE(5), PB(6), MV(7);

	private final int index;

	Layer(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}
}
