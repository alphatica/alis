package com.alphatica.alis.condition;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.FloatArraySlice;

import static com.alphatica.alis.data.layer.Layer.HIGH;

public class HighestHigh implements Condition {

	private final int length;

	public HighestHigh(int length) {
		this.length = length;
	}

	@Override
	public boolean matches(TimeMarketData marketData, TimeMarketDataSet allData) {
		FloatArraySlice closes = marketData.getLayer(HIGH);
		if (closes.size() < length) {
			return false;
		}
		double now = closes.get(0);
		for (int i = 1; i < length; i++) {
			if (now <= closes.get(i)) {
				return false;
			}
		}
		return true;
	}
}
