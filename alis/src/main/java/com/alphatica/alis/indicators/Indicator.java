package com.alphatica.alis.indicators;

import com.alphatica.alis.data.time.TimeMarketData;

public abstract class Indicator {
	protected int offset;

	public abstract double calculate(TimeMarketData marketData);

	public Indicator withOffset(int offset) {
		this.offset = offset;
		return this;
	}
}