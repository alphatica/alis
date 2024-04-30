package com.alphatica.alis.indicators.trend;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.indicators.Indicator;
import com.alphatica.alis.tools.data.DoubleArraySlice;

public class Sma extends Indicator {
	private final int length;

	public Sma(int length) {
		this.length = length;
	}

	@Override
	public double calculate(TimeMarketData marketData) {
		DoubleArraySlice closes = marketData.getLayer(Layer.CLOSE);
		if (closes.size() < length + offset) {
			return Double.NaN;
		} else {
			double sum = 0.0;
			for (int i = offset; i < length + offset; i++) {
				sum += closes.get(i);
			}
			return sum / length;
		}
	}
}
