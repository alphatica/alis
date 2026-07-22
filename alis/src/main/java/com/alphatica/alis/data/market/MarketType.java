package com.alphatica.alis.data.market;

public enum MarketType {
	STOCK, INDEX, FOREX, FUTURE
	;


	@Override
	public String toString() {
		return switch (this) {
			case STOCK -> "Stock";
			case INDEX -> "Index";
			case FOREX -> "Forex";
			case FUTURE -> "Future";
		};
	}
}
