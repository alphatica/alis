package com.alphatica.alis.data;

import com.alphatica.alis.data.market.Delisting;
import com.alphatica.alis.data.market.Delistings;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.data.time.TimeMarketDataSetCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.function.Predicate;

public class StandardMarketData implements MarketData {
	private final Map<MarketName, Market> markets;
	private final TimeMarketDataSetCache snapshotCache;
	private List<Time> times;
	private Delistings delistings;

	public StandardMarketData() {
		markets = new HashMap<>();
		snapshotCache = new TimeMarketDataSetCache(this);
		times = new ArrayList<>();
		delistings = Delistings.empty();
	}

	public void addMarkets(Map<MarketName, Market> newMarkets) {
		markets.putAll(newMarkets);
		Set<Time> timeHashSet = new HashSet<>();
		for (Market stock : markets.values()) {
			timeHashSet.addAll(stock.getTimes());
		}
		this.times = timeHashSet.stream().sorted().toList();
		this.delistings = new Delistings(delistings.all(), times);
		snapshotCache.clear();
	}

	public void setDelistings(List<Delisting> newDelistings) {
		this.delistings = new Delistings(newDelistings, times);
	}

	@Override
	public List<Time> getTimes() {
		return times;
	}

	@Override
	public Market getMarket(MarketName marketName) {
		return markets.get(marketName);
	}

	@Override
	public List<Market> listMarkets(Predicate<Market> filter) {
		return markets.values().stream().filter(filter).map(Market.class::cast).toList();
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


}
