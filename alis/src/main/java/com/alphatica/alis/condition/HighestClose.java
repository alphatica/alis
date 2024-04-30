package com.alphatica.alis.condition;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.BarsSinceHighestClose;

public class HighestClose implements Condition {

	private final BarsSinceHighestClose barsSinceHighestClose;

	public HighestClose(int length) {
		this.barsSinceHighestClose = new BarsSinceHighestClose(length);
	}

	@Override
	public boolean matches(TimeMarketData marketData, TimeMarketDataSet allData) {
		return barsSinceHighestClose.calculate(marketData) == 0.0;
	}

}
