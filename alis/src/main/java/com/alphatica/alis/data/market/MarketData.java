package com.alphatica.alis.data.market;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.data.time.TimeMarketDataSetCache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.alphatica.alis.data.market.MarketFilters.ALL;

public interface MarketData {
	List<Time> getTimes();

	Market getMarket(MarketName marketName);

	List<Market> listMarkets(Predicate<Market> filter);

	default TimeMarketDataSet snapshotAt(Time time) {
		List<Market> markets = listMarkets(ALL);
		Map<MarketName, TimeMarketData> result = HashMap.newHashMap(markets.size());
		for (Market market : markets) {
			TimeMarketData timeData = market.getAtOrPrevious(time);
			if (timeData != null) {
				result.put(market.getName(), timeData);
			}
		}
		return new TimeMarketDataSet(result, time);
	}

	default TimeMarketDataSet cachedSnapshotAt(Time time) {
		return snapshotAt(time);
	}

	default MarketData fromSingle(Market market) {
		return new MarketData() {
			private final TimeMarketDataSetCache snapshotCache = new TimeMarketDataSetCache(this);

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

			@Override
			public TimeMarketDataSet cachedSnapshotAt(Time time) {
				return snapshotCache.get(time);
			}
		};
	}

}
