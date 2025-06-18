package com.alphatica.alis.indicators.trend;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.indicators.Indicator;
import com.alphatica.alis.tools.data.FloatArraySlice;


public class MinMax extends Indicator {
	private final int length;

	public MinMax(int length) {
		this.length = length;
	}

	@Override
	public float calculate(TimeMarketData marketData) {
		FloatArraySlice closes = marketData.getLayer(Layer.CLOSE);
		if (closes.size() < length + offset) {
			return Float.NaN;
		} else {
			float max = closes.get(offset);
			int indexMax = 0;
			float min = closes.get(offset);
			int indexMin = 0;
			for (int i = 1 + offset; i < length + offset; i++) {
				float now = closes.get(i);
				if (now < min) {
					min = now;
					indexMin = i;
				}
				if (now > max) {
					max = now;
					indexMax = i;
				}
			}
			return ((float) indexMin - indexMax) / length;
		}
	}
}
