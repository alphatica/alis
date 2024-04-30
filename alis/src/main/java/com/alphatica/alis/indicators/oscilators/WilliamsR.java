package com.alphatica.alis.indicators.oscilators;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.indicators.Indicator;
import com.alphatica.alis.tools.data.DoubleArraySlice;

import static com.alphatica.alis.data.layer.Layer.CLOSE;

public class WilliamsR extends Indicator {

	private final int length;

	public WilliamsR(int length) {
		this.length = length;
	}

	@Override
	public double calculate(TimeMarketData marketData) {
		DoubleArraySlice closes = marketData.getLayer(CLOSE);
		if (closes.size() < length + offset) {
			return Double.NaN;
		} else {
			double max = closes.get(offset);
			double min = closes.get(offset);
			for (int i = 1 + offset; i < length + offset; i++) {
				double now = closes.get(i);
				if (now < min) {
					min = now;
				} else if (now > max) {
					max = now;
				}
			}
			double range = max - min;
			double diff = closes.get(offset) - min;
			return diff / range;
		}
	}
}
