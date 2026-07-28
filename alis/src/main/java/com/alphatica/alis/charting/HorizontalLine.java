package com.alphatica.alis.charting;

public record HorizontalLine(String name, double value) {

	public HorizontalLine {
		name = name == null ? null : name.strip();
	}
}
