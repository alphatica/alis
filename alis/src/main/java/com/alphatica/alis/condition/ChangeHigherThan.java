package com.alphatica.alis.condition;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.FloatArraySlice;

import static com.alphatica.alis.data.layer.Layer.CLOSE;

public class ChangeHigherThan implements Condition {

	private final int length;
	private final double minChangeRatio;

	public ChangeHigherThan(double minChange, int length) {
		this.length = length;
		minChangeRatio = minChange / 100.0 + 1;
	}

	@Override
	public boolean matches(TimeMarketData market, TimeMarketDataSet allMarkets) {
		FloatArraySlice closes = market.getLayer(CLOSE);
		if (closes.size() <= length) {
			return false;
		}
		double change = closes.get(0) / closes.get(length);
		return change > minChangeRatio;
	}
}
