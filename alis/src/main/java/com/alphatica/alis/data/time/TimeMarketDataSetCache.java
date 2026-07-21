package com.alphatica.alis.data.time;

import com.alphatica.alis.data.market.MarketData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class TimeMarketDataSetCache {
	private final MarketData marketData;
	private final Map<Time, TimeMarketDataSet> snapshots = new ConcurrentHashMap<>(1024);

	public TimeMarketDataSetCache(MarketData marketData) {
		this.marketData = marketData;
	}

	public TimeMarketDataSet get(Time time) {
		return snapshots.computeIfAbsent(time, marketData::snapshotAt);
	}

	public void clear() {
		snapshots.clear();
	}
}
