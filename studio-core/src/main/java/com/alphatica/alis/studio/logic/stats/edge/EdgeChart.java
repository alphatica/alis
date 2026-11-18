package com.alphatica.alis.studio.logic.stats.edge;

import java.util.List;

/**
 * Lines over the sessions.
 *
 * @param times yyyymmdd of every session
 */
public record EdgeChart(List<Integer> times, List<EdgeSeries> lines, Scale scale) {

	public enum Scale {
		/** The values are 100 × ln of a ratio; shown as how many percent one is ahead of the other. */
		RELATIVE
	}
}
