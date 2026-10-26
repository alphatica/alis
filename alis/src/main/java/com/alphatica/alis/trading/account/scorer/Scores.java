package com.alphatica.alis.trading.account.scorer;

final class Scores {
	private Scores() {
	}

	/**
	 * Optimizers need finite scores: an undefined ratio (NaN) or an infinite one scores 0.
	 */
	static double finiteOrZero(double value) {
		return Double.isFinite(value) ? value : 0;
	}
}
