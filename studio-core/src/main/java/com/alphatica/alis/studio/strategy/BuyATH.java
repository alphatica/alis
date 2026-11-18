package com.alphatica.alis.studio.strategy;

import com.alphatica.alis.condition.AllTimeHigh;
import com.alphatica.alis.condition.Condition;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.data.FloatArraySlice;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;
import com.alphatica.alis.trading.optimizer.params.IntParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;
import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;
import static com.alphatica.alis.trading.order.OrderSize.PERCENTAGE;

public class BuyATH extends Strategy {

	@IntParam(start = 5, step = 1, end = 250)
	int sessions = 59;

	/** Sessions in a month: the growth over them sets the position size */
	private static final int MONTH = 20;
	/** Position size in percent of the account for no growth over the month */
	private static final double MAX_SIZE = 10;
	/** Position size in percent of the account for {@link #HIGH_GROWTH} or more over the month */
	private static final double MIN_SIZE = 5;
	private static final double HIGH_GROWTH = 0.3;
	private final Map<MarketName, Integer> counters = new HashMap<>();
	private final Condition allTimeHigh = new AllTimeHigh();

	@Override
	public List<Order> afterClose(TimeMarketDataSet allData, Account account) {
		List<Order> orders = new ArrayList<>();
		for (TimeMarketData marketData : allData.listUpToDateMarkets(STOCKS)) {
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
		if (closes.size() > sessions) {
			return closes.get(0) / closes.get(sessions);
		} else {
			return closes.get(0) / closes.get(closes.size() - 1);
		}
	}

	/** From {@link #MAX_SIZE} at no growth over the last month down to {@link #MIN_SIZE} at {@link #HIGH_GROWTH}, linearly. */
	private static double positionSize(FloatArraySlice closes) {
		double growth = closes.get(0) / closes.get(Math.min(MONTH, closes.size() - 1)) - 1;
		return MAX_SIZE - (MAX_SIZE - MIN_SIZE) * Math.clamp(growth / HIGH_GROWTH, 0.0, 1.0);
	}

	private void checkSell(MarketName marketName, boolean havePosition, List<Order> orders) {
		int counterNow = counters.get(marketName) + 1;
		if (counterNow > sessions && havePosition) {
			orders.add(new Order(marketName, SELL, PERCENTAGE, 100, 0.0));
		}
		counters.put(marketName, counterNow);
	}

	private void checkBuy(MarketName marketName, FloatArraySlice closes, boolean havePosition, List<Order> orders) {
		counters.put(marketName, 0);
		if (!havePosition) {
			double priority = orderPriority(closes);
			orders.add(new Order(marketName, BUY, PERCENTAGE, positionSize(closes), priority));
		}
	}

}
