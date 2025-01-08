package com.alphatica.alis.trading.strategy.optimizer;

public enum ParametersSelection {

	FULL_PERMUTATION("Full permutation"), GENETIC("Genetic algorithm"), RANDOM("Random");

	private final String text;

	ParametersSelection(String s) {
		this.text = s;
	}

	public static ParametersSelection getByText(String string) {
		for (ParametersSelection rv : ParametersSelection.values()) {
			if (rv.text.equals(string)) {
				return rv;
			}
		}
		throw new IllegalArgumentException("No matching ParametersSelection found for text: " + string);
	}

	public String getText() {
		return text;
	}
}
