package com.alphatica.alis.data.market;

public enum MarketType {
	STOCK, INDICE, FOREX,
	;


	@Override
	public String toString() {
		return switch (this) {
			case STOCK -> "Stock";
			case INDICE -> "Indice";
			case FOREX -> "Forex";
		};
	}
}
