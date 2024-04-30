package com.alphatica.alis.data.market;

import com.alphatica.alis.data.time.Time;

import java.util.List;
import java.util.function.Predicate;

public interface MarketData {
	List<Time> getTimes();

	Market getMarket(MarketName marketName);

	List<Market> listMarkets(Predicate<Market> filter);

	default MarketData fromSingle(Market market) {
		return new MarketData() {

			@Override
			public List<Time> getTimes() {
				return market.getTimes();
			}

			@Override
			public Market getMarket(MarketName marketName) {
				if (marketName.equals(market.getName())) {
					return market;
				} else {
					return null;
				}
			}

			@Override
			public List<Market> listMarkets(Predicate<Market> filter) {
				if (filter.test(market)) {
					return List.of(market);
				} else {
					return List.of();
				}
			}
		};
	}

}
