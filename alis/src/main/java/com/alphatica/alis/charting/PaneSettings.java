package com.alphatica.alis.charting;

import java.util.List;

import static java.util.Objects.requireNonNull;

public record PaneSettings(
		double heightWeight,
		String yAxisTitle,
		List<HorizontalLine> horizontalLines) {

	public PaneSettings {
		if (!Double.isFinite(heightWeight) || heightWeight <= 0.0) {
			throw new IllegalArgumentException("heightWeight must be finite and greater than zero");
		}
		horizontalLines = List.copyOf(requireNonNull(horizontalLines, "horizontalLines"));
	}

	public static PaneSettings defaults() {
		return new PaneSettings(1.0, null, List.of());
	}
}
