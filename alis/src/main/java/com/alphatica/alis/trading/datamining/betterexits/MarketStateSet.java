package com.alphatica.alis.trading.datamining.betterexits;

import com.alphatica.alis.data.market.MarketName;

import java.util.HashMap;
import java.util.Map;

public class MarketStateSet {

	private final Map<MarketName, DoubleValueState> states = new HashMap<>();

	DoubleValueState get(MarketName marketName) {
		return states.computeIfAbsent(marketName, ignored -> new DoubleValueState());
	}

	public void delete(MarketName market) {
		states.remove(market);
	}
}
