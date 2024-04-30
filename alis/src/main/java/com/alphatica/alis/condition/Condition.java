package com.alphatica.alis.condition;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;

public interface Condition {
	static Condition all(Condition... conditions) {
		return (market, allData) -> {
			for (Condition condition : conditions) {
				if (!condition.matches(market, allData)) {
					return false;
				}
			}
			return true;
		};
	}

	static Condition not(Condition condition) {
		return (market, allMarkets) -> !condition.matches(market, allMarkets);
	}

	static Condition alwaysMatches() {
		return (market, allMarkets) -> true;
	}

	boolean matches(TimeMarketData market, TimeMarketDataSet allMarkets);
}
