package com.alphatica.alis.examples.timing;

import com.alphatica.alis.data.time.TimeMarketData;

import java.util.function.Supplier;

public interface Signal {

	boolean shouldBuy(TimeMarketData marketData);

	static boolean getOrFalse(Supplier<Boolean> supplier) {
		try {
			return supplier.get();
		} catch (Exception ex) {
			return false;
		}
	}
}
