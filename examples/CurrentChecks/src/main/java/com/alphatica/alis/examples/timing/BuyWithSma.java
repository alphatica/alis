package com.alphatica.alis.examples.timing;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.indicators.trend.Sma;

public class BuyWithSma implements Signal {
	private final Sma sma;
	private final boolean above;

	public BuyWithSma(int smaLen, boolean above) {
		this.sma = new Sma(smaLen);
		this.above = above;
	}

	@Override
	public boolean shouldBuy(TimeMarketData marketData) {
		double smaNow = sma.calculate(marketData);
		return above == (marketData.getData(Layer.CLOSE, 0) > smaNow);
	}
}
