package com.alphatica.alis.data.time;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class TimeMarketDataSet {
	private static final Map<Time, TimeMarketDataSet> cache = new ConcurrentHashMap<>(1024);
	private final Map<MarketName, TimeMarketData> set;
	private final Time time;

	private static MarketData cachedMarketData;

	public TimeMarketDataSet(Map<MarketName, TimeMarketData> set, Time time) {
		this.set = set;
		this.time = time;
	}

	public static TimeMarketDataSet getCached(Time time, MarketData marketData) {
		if (marketData != cachedMarketData) {
			cachedMarketData = marketData;
			cache.clear();
		}
		return cache.computeIfAbsent(time, marketData::snapshotAt);
	}

	public TimeMarketData get(MarketName name) {
		return set.get(name);
	}

	public Time getTime() {
		return time;
	}

	public List<TimeMarketData> listUpToDateMarkets(Predicate<TimeMarketData> filter) {
		return set.values().stream().filter(filter).filter(m -> m.getTime().equals(time)).toList();
	}

	public List<TimeMarketData> listAllKnownMarkets(Predicate<TimeMarketData> filter) {
		return set.values().stream().filter(filter).toList();
	}
}
