package com.alphatica.alis.studio.logic.table;

/**
 * Column of a table shown in the GUI. Cells are values, not text: the GUI formats them in the user's locale.
 * TEXT cells are strings, DATE cells yyyymmdd numbers, NUMBER cells numbers shown with {@code decimals} fraction
 * digits followed by {@code suffix}. An empty cell is null.
 */
public record Column(String title, Kind kind, int decimals, String suffix) {

	public enum Kind {
		TEXT, DATE, NUMBER
	}

	public static Column text(String title) {
		return new Column(title, Kind.TEXT, 0, "");
	}

	public static Column date(String title) {
		return new Column(title, Kind.DATE, 0, "");
	}

	public static Column number(String title, int decimals) {
		return new Column(title, Kind.NUMBER, decimals, "");
	}

	public static Column percent(String title, int decimals) {
		return new Column(title, Kind.NUMBER, decimals, " %");
	}
}
