package com.alphatica.alis.data.layer;

public enum Layer {
	OPEN(0), HIGH(1), LOW(2), CLOSE(3), VOLUME(4), TURNOVER(5), PE(6), PB(7), MV(8);

	private final int index;

	Layer(int i) {
		index = i;
	}

	public int getIndex() {
		return index;
	}
}
