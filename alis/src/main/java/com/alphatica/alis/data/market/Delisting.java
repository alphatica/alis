package com.alphatica.alis.data.market;

import com.alphatica.alis.data.time.Time;

import static java.util.Objects.requireNonNull;

public record Delisting(MarketName market, Time time, double price) {
	public Delisting {
		requireNonNull(market, "market");
		requireNonNull(time, "time");
	}
}
