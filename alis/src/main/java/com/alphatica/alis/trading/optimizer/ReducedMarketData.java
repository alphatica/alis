package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.data.market.Delisting;
import com.alphatica.alis.data.market.Delistings;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketDataSet;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * Market data without some markets. Cached snapshots are views of the base data's cached snapshots, so market data is
 * shared with the base, while values cached in snapshots belong to this instance.
 */
final class ReducedMarketData implements MarketData {
	private final MarketData base;
	private final Set<MarketName> excluded;
	private final List<Time> times;
	private final Delistings delistings;
	private final Map<Time, TimeMarketDataSet> snapshotCache = new ConcurrentHashMap<>(1024);

	ReducedMarketData(MarketData base, Set<MarketName> excluded) {
		this.base = base;
		this.excluded = Set.copyOf(excluded);
		this.times = collectTimes(listMarkets(market -> true));
		this.delistings = new Delistings(base.getDelistings(), times);
	}

	@Override
	public List<Time> getTimes() {
		return times;
	}

	@Override
	public Market getMarket(MarketName marketName) {
		return excluded.contains(marketName) ? null : base.getMarket(marketName);
	}

	@Override
	public List<Market> listMarkets(Predicate<Market> filter) {
		return base.listMarkets(market -> !excluded.contains(market.getName()) && filter.test(market));
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
		return snapshotCache.computeIfAbsent(time, t -> base.cachedSnapshotAt(t).without(excluded));
	}

	private static List<Time> collectTimes(List<Market> markets) {
		Set<Time> result = new HashSet<>();
		for (Market market : markets) {
			result.addAll(market.getTimes());
		}
		return result.stream().sorted().toList();
	}
}
