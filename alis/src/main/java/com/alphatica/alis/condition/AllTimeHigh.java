package com.alphatica.alis.condition;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.DoubleArraySlice;

import static com.alphatica.alis.data.layer.Layer.HIGH;

public class AllTimeHigh implements Condition {

	@Override
	public boolean matches(TimeMarketData market, TimeMarketDataSet allMarkets) {
		DoubleArraySlice prices = market.getLayer(HIGH);
		double now = prices.get(0);
		for (int i = 1; i < prices.size(); i++) {
			if (prices.get(i) > now) {
				return false;
			}
		}
		return true;
	}
}
