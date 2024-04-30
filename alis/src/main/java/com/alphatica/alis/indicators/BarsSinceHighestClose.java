package com.alphatica.alis.indicators;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.tools.data.DoubleArraySlice;

import static com.alphatica.alis.data.layer.Layer.CLOSE;

public class BarsSinceHighestClose extends Indicator {

	private final int length;

	public BarsSinceHighestClose(int length) {
		this.length = length;
	}

	@Override
	public double calculate(TimeMarketData marketData) {
		DoubleArraySlice closes = marketData.getLayer(CLOSE);
		if (closes.size() < length + offset) {
			return Double.NaN;
		}
		int found = 0;
		double value = closes.get(offset);
		for (int i = offset + 1; i < length + offset; i++) {
			if (closes.get(i) > value) {
				found = i;
				value = closes.get(i);
			}
		}
		return found;
	}

}
