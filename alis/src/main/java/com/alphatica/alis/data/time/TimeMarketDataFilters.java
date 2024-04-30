package com.alphatica.alis.data.time;

import com.alphatica.alis.data.market.MarketType;

import java.util.function.Predicate;

public class TimeMarketDataFilters {
	public static final Predicate<TimeMarketData> STOCKS = data -> data.getMarketType() == MarketType.STOCK;
	public static final Predicate<TimeMarketData> ALL = data -> true;

	private TimeMarketDataFilters() {
	}

}
