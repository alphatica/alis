package com.alphatica.alis.data.loader.stooq;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.tools.data.DoubleArraySlice;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class StooqMarket implements Market {
	private final TreeMap<Time, List<DoubleArraySlice>> ranges;
	private final MarketName name;
	private final MarketType marketType;

	@SuppressWarnings("java:S1319")
	public StooqMarket(TreeMap<Time, List<DoubleArraySlice>> ranges, MarketName name, MarketType marketType) {
		this.ranges = ranges;
		this.name = name;
		this.marketType = marketType;
	}

	@Override
	public List<Time> getTimes() {
		return ranges.keySet().stream().sorted().toList();
	}

	@Override
	public MarketName getName() {
		return name;
	}

	@Override
	public MarketType getType() {
		return marketType;
	}

	@Override
	public TimeMarketData getAtOrPrevious(Time time) {
		Map.Entry<Time, List<DoubleArraySlice>> entry = ranges.floorEntry(time);
		if (entry != null) {
			return new TimeMarketData(name, marketType, entry.getKey(), entry.getValue());
		} else {
			return null;
		}
	}

	@Override
	public TimeMarketData getAtOrNext(Time time) {
		Map.Entry<Time, List<DoubleArraySlice>> entry = ranges.ceilingEntry(time);
		if (entry != null) {
			return new TimeMarketData(name, marketType, entry.getKey(), entry.getValue());
		} else {
			return null;
		}
	}

	@Override
	public TimeMarketData getAt(Time time) {
		List<DoubleArraySlice> data = ranges.get(time);
		if (data == null) {
			return null;
		} else {
			return new TimeMarketData(name, marketType, time, data);
		}
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		StooqMarket that = (StooqMarket) o;
		return Objects.equals(name, that.name) && marketType == that.marketType;
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, marketType);
	}
}
