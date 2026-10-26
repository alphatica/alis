package com.alphatica.alis.studio.logic.table;

import com.alphatica.alis.trading.account.Quantities;

/**
 * Column of a table shown in the GUI. Cells are values, not text: the GUI formats them in the user's locale.
 * TEXT cells are strings, DATE cells yyyymmdd numbers, NUMBER cells numbers shown with {@code decimals} to
 * {@code maxDecimals} fraction digits followed by {@code suffix}. An empty cell is null.
 */
public record Column(String title, Kind kind, int decimals, int maxDecimals, String suffix) {

	public enum Kind {
		TEXT, DATE, NUMBER
	}

	public static Column text(String title) {
		return new Column(title, Kind.TEXT, 0, 0, "");
	}

	public static Column date(String title) {
		return new Column(title, Kind.DATE, 0, 0, "");
	}

	public static Column number(String title, int decimals) {
		return new Column(title, Kind.NUMBER, decimals, decimals, "");
	}

	public static Column percent(String title, int decimals) {
		return new Column(title, Kind.NUMBER, decimals, decimals, " %");
	}

	/**
	 * Number of shares: whole numbers without fraction digits, fractional ones with as many as needed.
	 */
	public static Column quantity(String title) {
		return new Column(title, Kind.NUMBER, 0, Quantities.DECIMAL_PLACES, "");
	}
}
