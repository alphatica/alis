package com.alphatica.alis.studio.logic.stats.edge;

import java.util.List;

/**
 * Bridge (waterfall) chart: the first step a total, then changes, then the last total, which is the first plus the
 * changes.
 *
 * @param title    shown above, or null
 * @param decimals fraction digits of the values
 * @param suffix   shown after the values, e.g. " %"
 */
public record EdgeBridge(String title, List<Step> steps, int decimals, String suffix) {

	static EdgeBridge of(String title, int decimals, String suffix, Step... steps) {
		return new EdgeBridge(title, List.of(steps), decimals, suffix);
	}

	/**
	 * @param value null when unknown (not finite)
	 * @param total a value drawn from 0, not a change
	 */
	public record Step(String name, Double value, boolean total) {

		static Step total(String name, double value) {
			return new Step(name, finiteOrNull(value), true);
		}

		static Step change(String name, double value) {
			return new Step(name, finiteOrNull(value), false);
		}

		private static Double finiteOrNull(double value) {
			return Double.isFinite(value) ? value : null;
		}
	}
}
