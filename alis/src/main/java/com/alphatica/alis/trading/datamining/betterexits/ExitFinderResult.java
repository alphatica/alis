package com.alphatica.alis.trading.datamining.betterexits;

import com.alphatica.alis.trading.account.Account;

public record ExitFinderResult(Account account, double score, int trades, String name, String description) implements Comparable<ExitFinderResult> {
	@Override
	public int compareTo(ExitFinderResult o) {
		int compare = Double.compare(this.score, o.score);
		if (compare == 0) {
			compare = this.name.compareTo(o.name);
		}
		return compare;
	}
}
