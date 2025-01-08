package com.alphatica.alis.data.loader;

import com.alphatica.alis.data.market.MarketData;

@FunctionalInterface
public interface ThrowingMarketDataSupplier {

	@SuppressWarnings("java:S112")
	MarketData get() throws Exception;
}
