package com.alphatica.alis.data.time;

import com.alphatica.alis.data.market.MarketName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public class TimeMarketDataSet {
	private final Map<MarketName, Integer> positions;
	private final TimeMarketData[] markets;
	private final Time time;
	private final Set<MarketName> excluded;
	private final Map<Object, Object> cache = new ConcurrentHashMap<>();

	public TimeMarketDataSet(Map<MarketName, TimeMarketData> set, Time time) {
		List<MarketName> names = set.keySet().stream().sorted().toList();
		Map<MarketName, Integer> namePositions = HashMap.newHashMap(names.size());
		TimeMarketData[] data = new TimeMarketData[names.size()];
		for (int i = 0; i < data.length; i++) {
			namePositions.put(names.get(i), i);
			data[i] = set.get(names.get(i));
		}
		this.positions = namePositions;
		this.markets = data;
		this.time = time;
		this.excluded = Set.of();
	}

	public TimeMarketDataSet(Map<MarketName, Integer> positions, TimeMarketData[] markets, Time time) {
		this(positions, markets, time, Set.of());
	}

	private TimeMarketDataSet(Map<MarketName, Integer> positions, TimeMarketData[] markets, Time time,
							  Set<MarketName> excluded) {
		this.positions = positions;
		this.markets = markets;
		this.time = time;
		this.excluded = excluded;
	}

	/**
	 * A view of this set without the given markets. Market data is shared with this set; the {@link #cached} values are
	 * not, because they may depend on which markets are present.
	 */
	public TimeMarketDataSet without(Set<MarketName> marketsToExclude) {
		if (excluded.isEmpty()) {
			// Set.copyOf returns an unmodifiable set as is, so views made with the same set share it
			return new TimeMarketDataSet(positions, markets, time, Set.copyOf(marketsToExclude));
		}
		Set<MarketName> allExcluded = new HashSet<>(excluded);
		allExcluded.addAll(marketsToExclude);
		return new TimeMarketDataSet(positions, markets, time, Set.copyOf(allExcluded));
	}

	public TimeMarketData get(MarketName name) {
		Integer position = positions.get(name);
		if (position == null || excluded.contains(name)) {
			return null;
		}
		return markets[position];
	}

	public Time getTime() {
		return time;
	}

	public List<TimeMarketData> listUpToDateMarkets(Predicate<TimeMarketData> filter) {
		List<TimeMarketData> result = new ArrayList<>();
		for (TimeMarketData marketData : markets) {
			if (marketData != null && marketData.getTime().equals(time) && isIncluded(marketData) && filter.test(marketData)) {
				result.add(marketData);
			}
		}
		return Collections.unmodifiableList(result);
	}

	public List<TimeMarketData> listAllKnownMarkets(Predicate<TimeMarketData> filter) {
		List<TimeMarketData> result = new ArrayList<>();
		for (TimeMarketData marketData : markets) {
			if (marketData != null && isIncluded(marketData) && filter.test(marketData)) {
				result.add(marketData);
			}
		}
		return Collections.unmodifiableList(result);
	}

	@SuppressWarnings("unchecked")
	public <T> T cached(Object key, Function<TimeMarketDataSet, T> compute) {
		return (T) cache.computeIfAbsent(key, ignored -> compute.apply(this));
	}

	private boolean isIncluded(TimeMarketData marketData) {
		return excluded.isEmpty() || !excluded.contains(marketData.getMarketName());
	}
}
