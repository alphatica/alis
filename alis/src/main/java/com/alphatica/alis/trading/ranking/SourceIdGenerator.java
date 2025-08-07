package com.alphatica.alis.trading.ranking;

import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("squid:S6548")
public enum SourceIdGenerator {
	SOURCE_ID;

	private final AtomicInteger counter = new AtomicInteger(0);

	public String next() {
		return String.valueOf(counter.incrementAndGet());
	}
}
