package com.alphatica.alis.trading.order;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;

public enum TradePrice {
	OPEN, AVERAGE;

	public double getPrice(TimeMarketData marketData) {
		return switch (this) {
			case OPEN -> marketData.getData(Layer.OPEN, 0);
			case AVERAGE -> marketData.getAveragePrice(0);
		};
	}
}
