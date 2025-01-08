package com.alphatica.alis.tools.java;

public class StringHelper {

	private StringHelper() {
	}

	public static String emptyOnNull(Object o) {
		return o == null ? "" : o.toString();
	}
}
