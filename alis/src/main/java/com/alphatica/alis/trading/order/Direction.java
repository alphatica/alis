package com.alphatica.alis.trading.order;

public enum Direction {
	BUY, SELL,
	;

	public static Direction fromString(String direction) {
		return switch (direction) {
			case "Buy" -> BUY;
			case "Sell" -> SELL;
			default -> null;
		};
	}

	@Override
	public String toString() {
		return switch (this) {
			case BUY -> "Buy";
			case SELL -> "Sell";
		};
	}
}
