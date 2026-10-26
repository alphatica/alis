package com.alphatica.alis.data.riskfreerate;

import java.util.Locale;

public enum Country {
	PL;

	/** Name of the rates file, the same for the built-in rates and the user's file. */
	String fileName() {
		return name().toLowerCase(Locale.ROOT) + ".csv";
	}
}
