package com.alphatica.alis.trading.account.scorer;

import com.alphatica.alis.trading.account.Account;

public record ScoredAccount(double score, Account account) implements Comparable<ScoredAccount> {
	@Override
	public int compareTo(ScoredAccount other) {
		return Double.compare(this.score, other.score);
	}
}
