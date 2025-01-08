package com.alphatica.alis.examples.timing;

import com.alphatica.alis.data.time.TimeMarketData;

public class AlwaysSignal implements Signal {
	@Override
	public boolean shouldBuy(TimeMarketData marketData) {
		return true;
	}
}
