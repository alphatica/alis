package com.alphatica.alis.trading.order;

/**
 * How {@link Order#sizeValue()} is read.
 */
public enum OrderSize {
	/**
	 * Buy: percent of account's NAV to spend. Sell: percent of the position to sell.
	 */
	PERCENTAGE,
	/**
	 * Number of shares.
	 */
	COUNT,
	/**
	 * Cash value: buy for this much (commission included), sell this much of the position (at most all of it).
	 */
	VALUE,
	;

	@Override
	public String toString() {
		return switch (this) {
			case PERCENTAGE -> "Percent (%)";
			case COUNT -> "Count";
			case VALUE -> "Value";
		};
	}

	public String shortSign() {
		return switch (this) {
			case PERCENTAGE -> "%";
			case COUNT -> "x";
			case VALUE -> " value";
		};
	}
}
