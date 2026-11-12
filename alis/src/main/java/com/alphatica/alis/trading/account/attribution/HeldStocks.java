package com.alphatica.alis.trading.account.attribution;

import com.alphatica.alis.data.market.Delisting;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.PositionPricesRecord;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.layer.Layer.OPEN;

public record HeldStocks(double[] versusBenchmark, double[] sizeEffect) {

	public static HeldStocks of(Account account, MarketData marketData, Market benchmark, List<Time> sessions) {
		int count = sessions.size();
		DailyChanges changes = new DailyChanges(count);
		for (Map.Entry<MarketName, Held> entry : held(account, marketData, sessions).entrySet()) {
			Market market = marketData.getMarket(entry.getKey());
			if (market != null) {
				entry.getValue().addChanges(market, sessions, changes);
			}
		}
		double[] benchmarkOpens = opens(benchmark, sessions, 0, count - 1, null);
		double[] versusBenchmark = new double[count];
		double[] sizeEffect = new double[count];
		for (int day = 0; day + 1 < count; day++) {
			double benchmarkChange = Math.log(benchmarkOpens[day + 1] / benchmarkOpens[day]);
			double equal = changes.equal(day);
			double weighted = changes.weighted(day);
			versusBenchmark[day + 1] = versusBenchmark[day]
					+ percentOrZero(equal - (Double.isFinite(benchmarkChange) ? benchmarkChange : 0));
			sizeEffect[day + 1] = sizeEffect[day] + percentOrZero(weighted - equal);
		}
		return new HeldStocks(versusBenchmark, sizeEffect);
	}

	private static double percentOrZero(double logChange) {
		return Double.isFinite(logChange) ? logChange * 100 : 0;
	}

	private static Map<MarketName, Held> held(Account account, MarketData marketData, List<Time> sessions) {
		Map<MarketName, Held> result = new HashMap<>();
		int last = sessions.size() - 1;
		for (PositionPricesRecord lot : account.getAllPricesRecords()) {
			if (lot.quantity() > 0) {
				Delisting delisting = lot.exitTime() == null ? null : delisting(lot, marketData);
				result.computeIfAbsent(lot.marketName(), ignored -> new Held()).add(lot, delisting, sessions, last);
			}
		}
		return result;
	}

	private static Delisting delisting(PositionPricesRecord lot, MarketData marketData) {
		for (Delisting delisting : marketData.getDelistingsAt(lot.exitTime())) {
			if (delisting.market().equals(lot.marketName()) && Double.compare(delisting.price(), lot.exit()) == 0) {
				return delisting;
			}
		}
		return null;
	}

	private static int indexAtOrAfter(List<Time> sessions, Time time) {
		int index = Collections.binarySearch(sessions, time);
		return index >= 0 ? index : -index - 1;
	}

	private static double[] opens(Market market, List<Time> sessions, int from, int to, Held held) {
		double[] result = new double[to - from + 1];
		for (int session = from; session <= to; session++) {
			result[session - from] = held != null && held.delisting != null && session > held.delistedAt
					? held.delisting.price()
					: open(market, sessions.get(session));
		}
		return result;
	}

	private static double open(Market market, Time time) {
		TimeMarketData data = market.getAt(time);
		if (data != null && data.getData(OPEN, 0) > 0) {
			return data.getData(OPEN, 0);
		}
		TimeMarketData previous = data == null ? market.getAtOrPrevious(time) : data;
		return previous == null ? Double.NaN : previous.getData(CLOSE, 0);
	}

	private record Lot(int entry, int exit, double capital) {
	}

	private static final class Held {
		private final BitSet days = new BitSet();
		private final List<Lot> lots = new ArrayList<>();
		private Delisting delisting;
		private int delistedAt;

		void add(PositionPricesRecord lot, Delisting lotDelisting, List<Time> sessions, int last) {
			int entry = indexAtOrAfter(sessions, lot.entryTime());
			int exit = lot.exitTime() == null ? last : Math.min(indexAtOrAfter(sessions, lot.exitTime()), last);
			if (lotDelisting != null && exit < last) {
				delisting = lotDelisting;
				delistedAt = exit;
				exit++;
			}
			if (entry < exit) {
				days.set(entry, exit);
				lots.add(new Lot(entry, exit, lot.quantity() * lot.entry()));
			}
		}

		void addChanges(Market market, List<Time> sessions, DailyChanges changes) {
			int first = days.nextSetBit(0);
			if (first < 0) {
				return;
			}
			double[] opens = opens(market, sessions, first, days.length(), this);
			double[] values = values(opens, first);
			for (int day = first; day >= 0; day = days.nextSetBit(day + 1)) {
				double change = opens[day + 1 - first] / opens[day - first] - 1;
				if (Double.isFinite(change)) {
					changes.add(day, change, values[day - first]);
				}
			}
		}

		private double[] values(double[] opens, int first) {
			double[] values = new double[opens.length];
			for (Lot lot : lots) {
				double entryOpen = opens[lot.entry() - first];
				for (int day = lot.entry(); day < lot.exit(); day++) {
					values[day - first] += lot.capital() * opens[day - first] / entryOpen;
				}
			}
			return values;
		}
	}

	private static final class DailyChanges {
		private final double[] sum;
		private final int[] stocks;
		private final double[] weightedSum;
		private final double[] value;

		DailyChanges(int count) {
			sum = new double[count];
			stocks = new int[count];
			weightedSum = new double[count];
			value = new double[count];
		}

		void add(int day, double change, double positionValue) {
			sum[day] += change;
			stocks[day]++;
			if (Double.isFinite(positionValue) && positionValue > 0) {
				weightedSum[day] += positionValue * change;
				value[day] += positionValue;
			}
		}

		double equal(int day) {
			return stocks[day] == 0 ? Double.NaN : Math.log1p(sum[day] / stocks[day]);
		}

		double weighted(int day) {
			return value[day] > 0 ? Math.log1p(weightedSum[day] / value[day]) : Double.NaN;
		}
	}
}
