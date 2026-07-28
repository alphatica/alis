package com.alphatica.alis.charting;

import java.awt.Color;
import java.awt.Font;

final class PaneTheme {

	static final Font LABEL_FONT = new Font("Monospaced", Font.PLAIN, 30);
	static final Font TITLE_FONT = new Font("Helvetica", Font.ITALIC, 70);
	static final Color[] SERIES_COLORS = {
			Color.WHITE, Color.GREEN, Color.ORANGE, Color.CYAN, Color.GRAY, Color.RED,
			Color.MAGENTA, Color.PINK, Color.YELLOW, Color.BLUE, Color.RED
	};

	private PaneTheme() {
	}
}
