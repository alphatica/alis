package com.alphatica.alis.condition;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;

public class HighestClose implements Condition {
	private final int length;

	public HighestClose(int length) {
		this.length = length;
	}

	@Override
	public boolean matches(TimeMarketData marketData, TimeMarketDataSet allData) {
		var closes = marketData.getLayer(Layer.CLOSE);
		if (closes.size() <= length) {
			return false;
		}
		var last = closes.get(0);
		for(int i = 1; i < length; i++) {
			if (closes.get(i) >= last) {
				return false;
			}
		}
		return true;
	}

}
