package com.alphatica.alis.indicators;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.tools.data.FloatArraySlice;

import static com.alphatica.alis.data.layer.Layer.CLOSE;

public class BarsSinceLowestClose extends Indicator {

	private final int length;

	public BarsSinceLowestClose(int length) {
		this.length = length;
	}

	@Override
	public float calculate(TimeMarketData marketData) {
		FloatArraySlice closes = marketData.getLayer(CLOSE);
		if (closes.size() < length + offset) {
			return Float.NaN;
		}
		int found = 0;
		float value = closes.get(offset);
		for (int i = 1 + offset; i < length + offset; i++) {
			if (closes.get(i) < value) {
				found = i;
				value = closes.get(i);
			}
		}
		return found;
	}
}
