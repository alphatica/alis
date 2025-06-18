package com.alphatica.alis.examples;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.tools.data.FloatArraySlice;

import java.io.File;
import java.util.concurrent.ExecutionException;

import static com.alphatica.alis.data.market.MarketFilters.STOCKS;

public class TenBagger {
	private static final String WORK_DIR = System.getProperty("user.home") + File.separator + "Alphatica" + File.separator + "stooq_gpw";

	public static void main(String[] args) throws ExecutionException, InterruptedException {
		showHeader();
		MarketData stooqData = StooqLoader.loadPL(WORK_DIR);
		for (Market market : stooqData.listMarkets(STOCKS)) {
//			checkMarketSinceIPO(market);
			checkMarket(market, new Time(0));
		}
	}

	private static void checkMarket(Market market, Time time) {
		int sessions = 0;
		double maxHighAfterLow = Double.MAX_VALUE;
		double maxDDAfterLow = 0;
		double currentDD;
		double lowestBeforeStart = Double.MAX_VALUE;
		Time lowestTime = time;
		TimeMarketData data = market.getAtOrNext(time);
		while (data != null) {
			double priceNow = data.getData(Layer.CLOSE, 0);
			if (priceNow < lowestBeforeStart) {
				maxHighAfterLow = priceNow;
				maxDDAfterLow = 0;
				lowestBeforeStart = priceNow;
				lowestTime = data.getTime();
				sessions = 0;
			}
			if (priceNow > maxHighAfterLow) {
				maxHighAfterLow = priceNow;
			}
			currentDD = (priceNow / maxHighAfterLow - 1) * 100;
			if (currentDD < maxDDAfterLow) {
				maxDDAfterLow = currentDD;
			}
			sessions++;
			double highNow = data.getData(Layer.CLOSE, 0);
			double change = highNow / lowestBeforeStart;
			if (change > 10.0) {
				showStats(market, lowestTime, lowestBeforeStart, data, sessions, maxDDAfterLow);
				break;
			}
			data = market.getAtOrNext(data.getTime().next());
		}
	}

	private static void checkMarketSinceIPO(Market market) {
		Time time = new Time(0);
		int sessions = 0;
		TimeMarketData data = market.getAtOrNext(time);
		if (data == null) {
			return;
		}
		double startPrice = data.getData(Layer.CLOSE, 0);
		Time startTime = data.getTime();
		double maxHighAfterIpo = Double.MIN_VALUE;
		double maxDDAfterIpo = 0;
		double currentDD;

		while (data != null) {
			double priceNow = data.getData(Layer.CLOSE, 0);
			if (priceNow > maxHighAfterIpo) {
				maxHighAfterIpo = priceNow;
			}
			currentDD = (priceNow / maxHighAfterIpo - 1) * 100;
			if (currentDD < maxDDAfterIpo) {
				maxDDAfterIpo = currentDD;
			}
			double change = priceNow / startPrice;
			if (change > 10.0) {
				showStats(market, startTime, startPrice, data, sessions, maxDDAfterIpo);
				break;
			}
			sessions++;
			data = market.getAtOrNext(data.getTime().next());
		}
	}

	private static void showHeader() {
		System.out.println("Name, Low Date, Low, 10x Date, High at 10x, Max DD during run, Sessions, Highest, Lowest, Last, Range Before 10x, " +
				"30% DDs, 50% DDs, 70% DDs, Price 1Y, Price 2Y, Price 3Y, Price 5Y");
	}

	private static void showStats(Market market, Time lowestTime, double lowestBeforeStart, TimeMarketData startData, int sessions,
								  double maxDDAfterLow) {
		TimeMarketData data = market.getAtOrNext(startData.getTime());
		double lowest = startData.getData(Layer.CLOSE, 0);
		double highest = startData.getData(Layer.CLOSE, 0);
		double last = startData.getData(Layer.CLOSE, 0);
		while (data != null) {
			last = data.getData(Layer.CLOSE, 0);
			double highNow = data.getData(Layer.CLOSE, 0);
			if (highNow > highest) {
				highest = highNow;
			}
			double lowNow = data.getData(Layer.CLOSE, 0);
			if (lowNow < lowest) {
				lowest = lowNow;
			}
			data = market.getAtOrNext(data.getTime().next());
		}
		System.out.printf("%s,%s,%.2f,%s,%.2f,%.0f,%d,%.2f,%.2f,%.2f", market.getName(), lowestTime, lowestBeforeStart, startData.getTime(),
				startData.getData(Layer.HIGH, 0), maxDDAfterLow, sessions, highest, lowest, last);
		double range = calcRange(startData);
		System.out.printf(",%.1f", range);
		int dd30Count = countDDs(0.7, market, startData.getTime());
		System.out.printf(",%d", dd30Count);
		int dd50Count = countDDs(0.5, market, startData.getTime());
		System.out.printf(",%d", dd50Count);
		int dd70Count = countDDs(0.3, market, startData.getTime());
		System.out.printf(",%d", dd70Count);
		Time oneYear = new Time(startData.getTime().time() + 10000);
		showIfPresent(market, oneYear);
		Time twoYears = new Time(startData.getTime().time() + 20000);
		showIfPresent(market, twoYears);
		Time threeYears = new Time(startData.getTime().time() + 30000);
		showIfPresent(market, threeYears);
		Time fiveYears = new Time(startData.getTime().time() + 50000);
		showIfPresent(market, fiveYears);
		System.out.println();
		checkMarket(market, startData.getTime());
	}

	private static int countDDs(double level, Market market, Time startTime) {
		TimeMarketData data = market.getAtOrNext(startTime);
		double high = data.getData(Layer.CLOSE, 0);
		int ddCounter = 0;
		boolean inDD = false;
		while (data != null) {
			if (data.getData(Layer.CLOSE, 0) > high) {
				high = data.getData(Layer.CLOSE, 0);
				inDD = false;
			}
			if (data.getData(Layer.CLOSE, 0) / high < level) {
				if (!inDD) {
					ddCounter++;
					inDD = true;
				}
			}
			data = market.getAtOrNext(data.getTime().next());
		}
		return ddCounter;
	}

	private static double calcRange(TimeMarketData startData) {
		double sum = 0;
		double count = 0;
		FloatArraySlice closes = startData.getLayer(Layer.CLOSE);
		int sessions = Math.min(20, closes.size() - 1);
		for (int i = 0; i < sessions; i++) {
			double change = (closes.get(i) / closes.get(i + 1) - 1.0) * 100;
			sum += Math.abs(change);
			count++;
		}
		return sum / count;
	}

	private static void showIfPresent(Market market, Time time) {
		TimeMarketData data = market.getAtOrNext(time);
		System.out.print(",");
		if (data != null) {
			double priceAtTime = data.getData(Layer.CLOSE, 0);
			System.out.printf("%.2f", priceAtTime);
		}
	}
}
