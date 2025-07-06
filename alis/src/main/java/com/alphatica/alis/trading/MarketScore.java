package com.alphatica.alis.trading;

import com.alphatica.alis.data.market.MarketName;

import java.util.Locale;


public record MarketScore(MarketName market, double value) implements Comparable<MarketScore> {
	@Override
	public int compareTo(MarketScore o) {
		return Double.compare(value, o.value);
	}

	@Override
	public String toString() {
		return String.format(Locale.US, "%s: %.2f", market.name(), value);
	}
}
