package com.alphatica.alis.studio.web;

import com.alphatica.alis.studio.logic.table.Column;
import com.alphatica.alis.trading.optimizer.OptimizerScore;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class OptimizationCsv {
	private OptimizationCsv() {
	}

	static String of(List<Column> columns, List<OptimizationResult> results) {
		List<String> params = results.isEmpty() ? List.of() : List.copyOf(results.getFirst().paramValues().keySet());
		List<String> lines = new ArrayList<>();
		lines.add(Stream.concat(columns.stream().map(Column::title), params.stream()).collect(Collectors.joining(",")));
		for (OptimizationResult result : results) {
			Stream<String> cells = result.cells().stream().map(OptimizationCsv::cellText);
			Stream<String> values = params.stream().map(name -> OptimizerScore.formatParamValue(result.paramValues().get(name)));
			lines.add(Stream.concat(cells, values).collect(Collectors.joining(",")));
		}
		return String.join("\n", lines) + "\n";
	}

	private static String cellText(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof Double number && Double.isFinite(number)) {
			return BigDecimal.valueOf(number).toPlainString();
		}
		return String.valueOf(value);
	}
}
