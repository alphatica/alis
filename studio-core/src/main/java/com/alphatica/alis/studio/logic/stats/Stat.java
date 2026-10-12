package com.alphatica.alis.studio.logic.stats;

/**
 * One value of a stats panel. The GUI formats numbers in the user's locale.
 *
 * @param value    a number, a text, or null when unknown (e.g. it needs market data)
 * @param decimals fraction digits of a number
 * @param suffix   shown after a number, e.g. " %"
 */
public record Stat(String name, Object value, int decimals, String suffix) {

	public static Stat number(String name, double value, int decimals) {
		return new Stat(name, value, decimals, "");
	}

	public static Stat percent(String name, double value, int decimals) {
		return new Stat(name, value, decimals, " %");
	}

	public static Stat text(String name, String value) {
		return new Stat(name, value, 0, "");
	}
}
