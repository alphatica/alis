package com.alphatica.alis.studio.logic.stats.edge;

import com.alphatica.alis.studio.logic.table.Column;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A small table of a card, shown whole.
 *
 * @param rows one value per column, see {@link Column}; numbers that are not finite are null
 */
public record EdgeTable(List<Column> columns, List<List<Object>> rows) {

	static List<Object> row(String name, double... values) {
		List<Object> row = new ArrayList<>(values.length + 1);
		row.add(name);
		for (double value : values) {
			row.add(Double.isFinite(value) ? value : null);
		}
		return Collections.unmodifiableList(row);
	}
}
