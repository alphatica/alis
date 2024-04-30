package com.alphatica.alis.condition;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.BarsSinceLowestClose;

public class LowestClose implements Condition {
	private final BarsSinceLowestClose barsSinceLowestClose;

	public LowestClose(int length) {
		this.barsSinceLowestClose = new BarsSinceLowestClose(length);
	}

	@Override
	public boolean matches(TimeMarketData marketData, TimeMarketDataSet allData) {
		return barsSinceLowestClose.calculate(marketData) == 0.0;
	}

}
