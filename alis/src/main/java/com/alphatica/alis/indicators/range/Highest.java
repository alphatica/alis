package com.alphatica.alis.indicators.range;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.indicators.Indicator;
import com.alphatica.alis.tools.data.FloatArraySlice;

public class Highest extends Indicator {

	private final Layer layer;
	private final int len;

	public Highest(Layer layer, int len) {
		this.layer = layer;
		this.len = len;
	}

	@Override
	public float calculate(TimeMarketData marketData) {
		FloatArraySlice values = marketData.getLayer(layer);
		if (values.size() < len + offset) {
			return Float.NaN;
		}
		float m = values.get(offset);
		for (int i = 1; i < len; i++) {
			if (values.get(i + offset) > m) {
				m = values.get(i + offset);
			}
		}
		return m;
	}
}
