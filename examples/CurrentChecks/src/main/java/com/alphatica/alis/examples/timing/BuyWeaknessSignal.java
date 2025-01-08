package com.alphatica.alis.examples.timing;

import com.alphatica.alis.data.time.TimeMarketData;

import static com.alphatica.alis.data.layer.Layer.CLOSE;

public class BuyWeaknessSignal implements Signal {

	private final int len;
	private final double change;

	public BuyWeaknessSignal(int len, double change) {
		this.len = len;
		this.change = 1 - change;
	}

	@Override
	public boolean shouldBuy(TimeMarketData marketData) {
		return Signal.getOrFalse(() -> marketData.getData(CLOSE, 0) / marketData.getData(CLOSE, len) < change);
	}
}
