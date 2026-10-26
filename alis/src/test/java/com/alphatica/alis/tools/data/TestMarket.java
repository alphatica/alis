package com.alphatica.alis.tools.data;

import com.alphatica.alis.data.FloatArraySlice;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public record TestMarket(MarketName name, TreeMap<Time, TimeMarketData> data) implements Market {

	/** A market quoted at the times (yyyymmdd, increasing) with the closes, which also serve as open, high and low. */
	public static TestMarket withCloses(MarketName name, int[] times, float[] closes) {
		float[] newestFirst = new float[closes.length];
		for (int i = 0; i < closes.length; i++) {
			newestFirst[i] = closes[closes.length - 1 - i];
		}
		TreeMap<Time, TimeMarketData> data = new TreeMap<>();
		for (int i = 0; i < times.length; i++) {
			FloatArraySlice prices = new FloatArraySlice(newestFirst, closes.length - 1 - i);
			Time time = new Time(times[i]);
			data.put(time, new TimeMarketData(name, MarketType.INDEX, time, List.of(prices, prices, prices, prices)));
		}
		return new TestMarket(name, data);
	}

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
