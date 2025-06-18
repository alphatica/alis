package com.alphatica.alis.indicators.oscilators;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.indicators.Indicator;
import com.alphatica.alis.tools.data.FloatArraySlice;

import static com.alphatica.alis.data.layer.Layer.CLOSE;

public class WilliamsR extends Indicator {

	private final int length;

	public WilliamsR(int length) {
		this.length = length;
	}

	@Override
	public float calculate(TimeMarketData marketData) {
		FloatArraySlice closes = marketData.getLayer(CLOSE);
		if (closes.size() < length + offset) {
			return Float.NaN;
		} else {
			float max = closes.get(offset);
			float min = closes.get(offset);
			for (int i = 1 + offset; i < length + offset; i++) {
				float now = closes.get(i);
				if (now < min) {
					min = now;
				} else if (now > max) {
					max = now;
				}
			}
			float range = max - min;
			float diff = closes.get(offset) - min;
			return diff / range;
		}
	}
}
