package com.alphatica.alis.data.loader.stooq;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketCursor;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;

import java.util.AbstractList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;

public class StooqMarket implements Market {
	private final MarketName name;
	private final MarketType marketType;
	private final int[] times;
	private final float[][] columns;
	private final List<Time> timeList;

	public StooqMarket(MarketName name, MarketType marketType, int[] times, float[][] columns) {
		for (float[] column : columns) {
			if (column.length != times.length) {
				throw new IllegalArgumentException("Every layer must have a value for every time");
			}
		}
		this.name = name;
		this.marketType = marketType;
		this.times = times;
		this.columns = columns;
		this.timeList = new TimeList(times);
	}

	@Override
	public List<Time> getTimes() {
		return timeList;
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
		return dataAt(floorIndex(time.time()));
	}

	@Override
	public TimeMarketData getAtOrNext(Time time) {
		int index = Arrays.binarySearch(times, time.time());
		return dataAt(index >= 0 ? index : -index - 1);
	}

	@Override
	public TimeMarketData getAt(Time time) {
		int index = Arrays.binarySearch(times, time.time());
		return index >= 0 ? dataAt(index) : null;
	}

	@Override
	public MarketCursor cursor() {
		return new Cursor();
	}

	private int floorIndex(int time) {
		int index = Arrays.binarySearch(times, time);
		return index >= 0 ? index : -index - 2;
	}

	private TimeMarketData dataAt(int index) {
		if (index < 0 || index >= times.length) {
			return null;
		}
		return new TimeMarketData(name, marketType, new Time(times[index]), columns, times.length - 1 - index);
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

	private final class Cursor implements MarketCursor {
		private int index = -1;
		private TimeMarketData current;

		@Override
		public TimeMarketData atOrPrevious(Time time) {
			int found = indexFor(time.time());
			if (found != index) {
				index = found;
				current = dataAt(found);
			}
			return current;
		}

		private int indexFor(int time) {
			if (index >= 0 && times[index] > time) {
				return floorIndex(time);
			}
			int following = index + 1;
			if (following >= times.length || times[following] > time) {
				return index;
			}
			if (following + 1 >= times.length || times[following + 1] > time) {
				return following;
			}
			return floorIndex(time);
		}
	}

	private static final class TimeList extends AbstractList<Time> implements RandomAccess {
		private final int[] times;

		TimeList(int[] times) {
			this.times = times;
		}

		@Override
		public Time get(int index) {
			return new Time(times[index]);
		}

		@Override
		public int size() {
			return times.length;
		}
	}
}
