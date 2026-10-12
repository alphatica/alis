package com.alphatica.alis.studio.logic.trading.signals;

import com.alphatica.alis.data.market.MarketAttributes;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.studio.logic.table.Column;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrategySummaryTableTest {

	@Test
	void shouldSortMarketsAndColumnsByName() {
		Map<MarketName, MarketAttributes> summary = new LinkedHashMap<>();
		summary.put(new MarketName("xyz"), () -> Map.of("Zeta", "a", "Alpha", "b"));
		summary.put(new MarketName("abc"), () -> Map.of("Alpha", "c"));

		StrategySummaryTable table = StrategySummaryTable.of(summary);

		assertEquals(List.of("Market", "Alpha", "Zeta"), table.columns().stream().map(Column::title).toList());
		assertArrayEquals(new Object[]{"abc", "c", null}, table.rows().get(0));
		assertArrayEquals(new Object[]{"xyz", "b", "a"}, table.rows().get(1));
	}

	@Test
	void shouldSendNumbersWithMostDecimalsFound() {
		Map<MarketName, MarketAttributes> summary = new LinkedHashMap<>();
		summary.put(new MarketName("abc"), () -> Map.of("Distance", "-1,5", "Age", "12"));
		summary.put(new MarketName("def"), () -> Map.of("Distance", "2.25", "Age", ""));

		StrategySummaryTable table = StrategySummaryTable.of(summary);

		assertEquals(Column.number("Age", 0), table.columns().get(1));
		assertEquals(Column.number("Distance", 2), table.columns().get(2));
		assertArrayEquals(new Object[]{"abc", 12.0, -1.5}, table.rows().get(0));
		assertArrayEquals(new Object[]{"def", null, 2.25}, table.rows().get(1));
	}

	@Test
	void shouldKeepPercentSign() {
		StrategySummaryTable table = StrategySummaryTable.of(Map.of(new MarketName("abc"), () -> Map.of("Change", "3.5 %")));

		assertEquals(Column.percent("Change", 1), table.columns().get(1));
		assertEquals(3.5, table.rows().getFirst()[1]);
	}

	@Test
	void shouldKeepTextWhenAnyValueIsNotANumber() {
		Map<MarketName, MarketAttributes> summary = new LinkedHashMap<>();
		summary.put(new MarketName("abc"), () -> Map.of("State", "12"));
		summary.put(new MarketName("def"), () -> Map.of("State", "waiting"));
		summary.put(new MarketName("ghi"), () -> Map.of("State", "5 %"));

		StrategySummaryTable table = StrategySummaryTable.of(summary);

		assertEquals(Column.text("State"), table.columns().get(1));
		assertEquals(List.of("12", "waiting", "5 %"), table.rows().stream().map(row -> row[1]).toList());
	}

	@Test
	void shouldMixPercentAndPlainNumbersAsText() {
		Map<MarketName, MarketAttributes> summary = new LinkedHashMap<>();
		summary.put(new MarketName("abc"), () -> Map.of("Value", "12"));
		summary.put(new MarketName("def"), () -> Map.of("Value", "5 %"));

		StrategySummaryTable table = StrategySummaryTable.of(summary);

		assertEquals(Column.Kind.TEXT, table.columns().get(1).kind());
	}

	@Test
	void shouldBeEmptyForStrategyWithoutSummary() {
		StrategySummaryTable table = StrategySummaryTable.of(Map.of());

		assertEquals(List.of(Column.text("Market")), table.columns());
		assertTrue(table.rows().isEmpty());
	}
}
