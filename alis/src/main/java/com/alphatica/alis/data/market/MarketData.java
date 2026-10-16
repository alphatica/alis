package com.alphatica.alis.data.market;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.data.time.TimeMarketDataSetCache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Predicate;

import static com.alphatica.alis.data.market.MarketFilters.ALL;

public interface MarketData {
	List<Time> getTimes();

	Market getMarket(MarketName marketName);

	List<Market> listMarkets(Predicate<Market> filter);

	default List<Delisting> getDelistings() {
		return List.of();
	}

	/**
	 * Delistings executed at the given session: the first session at or after the delisting date.
	 */
	default List<Delisting> getDelistingsAt(Time session) {
		return List.of();
	}

	/**
	 * Delistings grouped by session in which they are executed.
	 */
	default NavigableMap<Time, List<Delisting>> getDelistingsBySession() {
		TreeMap<Time, List<Delisting>> result = new TreeMap<>();
		for (Time time : getTimes()) {
			List<Delisting> delistings = getDelistingsAt(time);
			if (!delistings.isEmpty()) {
				result.put(time, delistings);
			}
		}
		return result;
	}

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
			private final Delistings delistings = new Delistings(MarketData.this.getDelistings().stream()
					.filter(delisting -> delisting.market().equals(market.getName()))
					.toList(), market.getTimes());

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
			public List<Delisting> getDelistings() {
				return delistings.all();
			}

			@Override
			public List<Delisting> getDelistingsAt(Time session) {
				return delistings.at(session);
			}

			@Override
			public NavigableMap<Time, List<Delisting>> getDelistingsBySession() {
				return delistings.bySession();
			}

			@Override
			public TimeMarketDataSet cachedSnapshotAt(Time time) {
				return snapshotCache.get(time);
			}
		};
	}

}
