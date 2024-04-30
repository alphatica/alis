package com.alphatica.alis.tools.data.loader;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record TestMarket(MarketName name, TreeMap<Time, TimeMarketData> data) implements Market {

	@Override
	public MarketName getName() {
		return name;
	}

	@Override
	public MarketType getType() {
		return MarketType.STOCK;
	}

	@Override
	public List<Time> getTimes() {
		return data.keySet().stream().sorted().toList();
	}

	@Override
	public TimeMarketData getAtOrPrevious(Time time) {
		Map.Entry<Time, TimeMarketData> entry = data.floorEntry(time);
		if (entry == null) {
			return null;
		} else {
			return entry.getValue();
		}
	}

	@Override
	public TimeMarketData getAtOrNext(Time time) {
		Map.Entry<Time, TimeMarketData> entry = data.ceilingEntry(time);
		if (entry == null) {
			return null;
		} else {
			return entry.getValue();
		}
	}

	@Override
	public TimeMarketData getAt(Time time) {
		return data.get(time);
	}
}
