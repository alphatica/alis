package com.alphatica.alis.examples.ath;

import com.alphatica.alis.charting.Chart;
import com.alphatica.alis.charting.LineChartData;
import com.alphatica.alis.condition.AllTimeHigh;
import com.alphatica.alis.condition.Condition;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.FloatArraySlice;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;
import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;
import static com.alphatica.alis.trading.order.OrderSize.PERCENTAGE;

public class AthPlusTimeStrategy extends Strategy {
	private static final int SESSIONS = 44;
	private static final int POSITIONS = 20;
	private final Map<MarketName, Integer> counters = new HashMap<>();
	private final Condition allTimeHigh = new AllTimeHigh();
	private final LineChartData<String> equityLine = new LineChartData<>();

	@Override
	public List<Order> afterClose(TimeMarketDataSet allData, Account account) {
		equityLine.addPoint(allData.getTime().toString(), account.getNAV());
		List<Order> orders = new ArrayList<>();
		for (TimeMarketData marketData : allData.listMarkets(STOCKS)) {
			counters.putIfAbsent(marketData.getMarketName(), 0);
			boolean havePosition = account.getPosition(marketData.getMarketName()) != null;
			if (allTimeHigh.matches(marketData, allData)) {
				checkBuy(marketData.getMarketName(), marketData.getLayer(CLOSE), havePosition, orders);
			} else {
				checkSell(marketData.getMarketName(), havePosition, orders);
			}
		}
		return orders;
	}

	private double orderPriority(FloatArraySlice closes) {
		if (closes.size() > SESSIONS) {
			return closes.get(0) / closes.get(SESSIONS);
		} else {
			return closes.get(0) / closes.get(closes.size() - 1);
		}
	}

	private void checkSell(MarketName marketName, boolean havePosition, List<Order> orders) {
		int counterNow = counters.get(marketName) + 1;
		if (counterNow > SESSIONS && havePosition) {
			orders.add(new Order(marketName, SELL, PERCENTAGE, 100, 0.0));
		}
		counters.put(marketName, counterNow);
	}

	private void checkBuy(MarketName marketName, FloatArraySlice closes, boolean havePosition, List<Order> orders) {
		counters.put(marketName, 0);
		if (!havePosition) {
			double priority = orderPriority(closes);
			orders.add(new Order(marketName, BUY, PERCENTAGE, 100 / POSITIONS, priority));
		}
	}

	@SuppressWarnings("java:S106") // Suppress warning about 'System.out.println'
	@Override
	public void finished(Account account) {
		Chart<String> chart = new Chart<>();
		equityLine.setConnectPoints(true);
		chart.addDataLines(List.of(equityLine));
		chart.setTitle("Buy at ATH, hold for 2 months");
		chart.setCopyright("Alphatica.com");
		try {
			chart.createImage(new File("athTime.png"));
		} catch (IOException e) {
			System.out.println("Unable to create chart: " + e.getMessage());
		}
	}
}
