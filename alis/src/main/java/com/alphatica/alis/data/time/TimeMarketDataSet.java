package com.alphatica.alis.data.time;

import com.alphatica.alis.data.market.MarketName;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;

public class TimeMarketDataSet {
	private final Map<MarketName, TimeMarketData> set;
	private final Time time;
	private final Set<MarketName> excluded;
	private final Map<Object, Object> cache = new ConcurrentHashMap<>();

	public TimeMarketDataSet(Map<MarketName, TimeMarketData> set, Time time) {
		this(set, time, Set.of());
	}

	private TimeMarketDataSet(Map<MarketName, TimeMarketData> set, Time time, Set<MarketName> excluded) {
		this.set = set;
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
			return new TimeMarketDataSet(set, time, Set.copyOf(marketsToExclude));
		}
		Set<MarketName> allExcluded = new HashSet<>(excluded);
		allExcluded.addAll(marketsToExclude);
		return new TimeMarketDataSet(set, time, Set.copyOf(allExcluded));
	}

	public TimeMarketData get(MarketName name) {
		if (excluded.contains(name)) {
			return null;
		}
		return set.get(name);
	}

	public Time getTime() {
		return time;
	}

	public List<TimeMarketData> listUpToDateMarkets(Predicate<TimeMarketData> filter) {
		return set.values().stream().filter(this::isIncluded).filter(filter).filter(m -> m.getTime().equals(time)).toList();
	}

	public List<TimeMarketData> listAllKnownMarkets(Predicate<TimeMarketData> filter) {
		return set.values().stream().filter(this::isIncluded).filter(filter).toList();
	}

	@SuppressWarnings("unchecked")
	public <T> T cached(Object key, Function<TimeMarketDataSet, T> compute) {
		return (T) cache.computeIfAbsent(key, ignored -> compute.apply(this));
	}

	private boolean isIncluded(TimeMarketData marketData) {
		return excluded.isEmpty() || !excluded.contains(marketData.getMarketName());
	}
}
