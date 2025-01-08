package com.alphatica.alis.trading.order;

public enum OrderSize {
	PERCENTAGE, COUNT,
	;

	@Override
	public String toString() {
		return switch (this) {
			case PERCENTAGE -> "Percent (%)";
			case COUNT -> "Count";
		};
	}
}
