package com.alphatica.alis.studio.logic.trading.signals;

import com.alphatica.alis.data.market.MarketAttributes;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.studio.logic.table.Column;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A strategy's summary table ({@code Strategy.getSummaryTable()}): one row per market, columns sorted by name.
 * Strategies give the values as text; a column whose non-empty values are all numbers (with '.' or ',' as the
 * decimal mark, optionally followed by '%') is sent as a number column, so the GUI shows it in the user's locale.
 */
public record StrategySummaryTable(List<Column> columns, List<Object[]> rows) {

	private static final Pattern NUMBER = Pattern.compile("(-?\\d+)(?:[.,](\\d+))?( ?%)?");

	public static StrategySummaryTable of(Map<MarketName, MarketAttributes> summary) {
		List<Map.Entry<MarketName, Map<String, String>>> markets = summary.entrySet().stream()
				.map(entry -> Map.entry(entry.getKey(), entry.getValue().toAttributes()))
				.sorted(Comparator.comparing(entry -> entry.getKey().name()))
				.toList();
		TreeSet<String> names = new TreeSet<>();
		markets.forEach(market -> names.addAll(market.getValue().keySet()));

		List<Column> columns = new ArrayList<>();
		columns.add(Column.text("Market"));
		List<Object[]> rows = markets.stream().map(market -> {
			Object[] row = new Object[names.size() + 1];
			row[0] = market.getKey().name();
			return row;
		}).toList();
		int index = 1;
		for (String name : names) {
			List<String> values = markets.stream().map(market -> market.getValue().get(name)).toList();
			columns.add(fillColumn(name, values, rows, index++));
		}
		return new StrategySummaryTable(columns, rows);
	}

	private static Column fillColumn(String name, List<String> values, List<Object[]> rows, int index) {
		Column column = numberColumn(name, values);
		for (int i = 0; i < values.size(); i++) {
			String value = values.get(i);
			rows.get(i)[index] = isEmpty(value) || column.kind() == Column.Kind.TEXT ? emptyToNull(value) : parse(value);
		}
		return column;
	}

	/**
	 * A number column with the most fraction digits found, or a text column when any value is not a number.
	 */
	private static Column numberColumn(String name, List<String> values) {
		int decimals = 0;
		Boolean percent = null;
		for (String value : values) {
			if (isEmpty(value)) {
				continue;
			}
			Matcher matcher = NUMBER.matcher(value.trim());
			boolean valuePercent = matcher.matches() && matcher.group(3) != null;
			if (!matcher.matches() || percent != null && percent != valuePercent) {
				return Column.text(name);
			}
			percent = valuePercent;
			decimals = Math.max(decimals, matcher.group(2) == null ? 0 : matcher.group(2).length());
		}
		if (percent == null) {
			return Column.text(name);
		}
		return percent ? Column.percent(name, decimals) : Column.number(name, decimals);
	}

	private static double parse(String value) {
		Matcher matcher = NUMBER.matcher(value.trim());
		if (!matcher.matches()) {
			throw new IllegalStateException("Not a number: " + value);
		}
		String fraction = matcher.group(2) == null ? "0" : matcher.group(2);
		return Double.parseDouble(matcher.group(1) + "." + fraction);
	}

	private static boolean isEmpty(String value) {
		return value == null || value.isBlank();
	}

	private static String emptyToNull(String value) {
		return isEmpty(value) ? null : value;
	}
}
