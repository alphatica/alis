package com.alphatica.alis.trading.strategy;

import com.alphatica.alis.trading.account.Account;

import static com.alphatica.alis.tools.java.NumberTools.percentChange;

final class StrategyExecutionLogger {

	private final boolean verbose;

	StrategyExecutionLogger(boolean verbose) {
		this.verbose = verbose;
	}

	void showPositions(Account account) {
		if (!verbose) {
			return;
		}
		for (var entry : account.getPositions().entrySet()) {
			var position = entry.getValue();
			log("Have position: %s x %d bought at %.2f profit: %.1f%% / %.1f",
					entry.getKey(), position.getQuantity(), position.getEntryPrice(),
					percentChange(position.getEntryPrice(), position.getLastClose()),
					position.getQuantity() * (position.getLastClose() - position.getEntryPrice()));
		}
	}

	@SuppressWarnings("java:S106")
	void log(String format, Object... args) {
		if (verbose) {
			System.out.printf(format + "%n", args);
		}
	}
}
