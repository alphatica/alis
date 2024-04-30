package com.alphatica.alis.condition;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;

public class AlwaysTrue implements Condition {

	@Override
	public boolean matches(TimeMarketData market, TimeMarketDataSet allMarkets) {
		return true;
	}
}
