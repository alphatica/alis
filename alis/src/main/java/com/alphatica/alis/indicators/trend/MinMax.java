package com.alphatica.alis.indicators.trend;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.indicators.Indicator;
import com.alphatica.alis.tools.data.DoubleArraySlice;


public class MinMax extends Indicator {
	private final int length;

	public MinMax(int length) {
		this.length = length;
	}

	@Override
	public double calculate(TimeMarketData marketData) {
		DoubleArraySlice closes = marketData.getLayer(Layer.CLOSE);
		if (closes.size() < length + offset) {
			return Double.NaN;
		} else {
			double max = closes.get(offset);
			int indexMax = 0;
			double min = closes.get(offset);
			int indexMin = 0;
			for (int i = 1 + offset; i < length + offset; i++) {
				double now = closes.get(i);
				if (now < min) {
					min = now;
					indexMin = i;
				}
				if (now > max) {
					max = now;
					indexMax = i;
				}
			}
			return ((double) indexMin - indexMax) / length;
		}
	}
}
