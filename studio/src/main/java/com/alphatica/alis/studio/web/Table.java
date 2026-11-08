package com.alphatica.alis.studio.web;

import com.alphatica.alis.studio.logic.table.Column;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Table sent to the GUI: either whole (small tables) or in pages ({@link #page(int, int)}, see {@link TableRoutes}).
 * The GUI renders only the visible rows in both cases.
 *
 * @param rows one value per column, see {@link Column}
 */
public record Table(List<Column> columns, List<Object[]> rows, String emptyMessage) {

	public TablePage page(int offset, int limit) {
		int from = Math.min(offset, rows.size());
		int to = Math.min(rows.size(), from + limit);
		return new TablePage(columns, rows.size(), from, rows.subList(from, to), emptyMessage);
	}

	public Table sortedBy(int column, boolean descending) {
		if (column < 0 || column >= columns.size() || columns.get(column).kind() == Column.Kind.TEXT) {
			throw new ValidationException("Column " + column + " cannot be sorted");
		}
		Comparator<Double> order = descending ? Comparator.reverseOrder() : Comparator.naturalOrder();
		List<Object[]> sorted = new ArrayList<>(rows);
		sorted.sort(Comparator.comparing(row -> sortKey(row[column]), Comparator.nullsLast(order)));
		return new Table(columns, sorted, emptyMessage);
	}

	private static Double sortKey(Object value) {
		return value instanceof Number number && !Double.isNaN(number.doubleValue()) ? number.doubleValue() : null;
	}

	/**
	 * Rows {@code offset .. offset + rows.size()} of a table with {@code total} rows.
	 */
	public record TablePage(List<Column> columns, int total, int offset, List<Object[]> rows, String emptyMessage) {
	}
}
