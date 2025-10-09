package com.alphatica.alis.data.market;

public enum MarketType {
	STOCK, INDICE, FOREX, FUTURE
	;


	@Override
	public String toString() {
		return switch (this) {
			case STOCK -> "Stock";
			case INDICE -> "Indice";
			case FOREX -> "Forex";
			case FUTURE -> "Future";
		};
	}
}
