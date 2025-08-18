package com.alphatica.alis.examples.minmax;

import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.trend.MinMax;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;

import java.util.ArrayList;
import java.util.List;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;
import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;
import static com.alphatica.alis.trading.order.OrderSize.PERCENTAGE;

public class MinMaxStrategy extends Strategy {
	private final MinMax minMax;

	public MinMaxStrategy(int length) {
		this.minMax = new MinMax(length);
	}

	@Override
	public List<Order> afterClose(TimeMarketDataSet data, Account account) {
		List<Order> orders = new ArrayList<>();
		for (TimeMarketData market : data.listUpToDateMarkets(STOCKS)) {
			double minMaxNow = minMax.calculate(market);
			boolean positionOpened = account.getPosition(market.getMarketName()) != null;
			if (!positionOpened && minMaxNow > 0) {
				orders.add(new Order(market.getMarketName(), BUY, PERCENTAGE, 100, 1.0));
			}
			if (positionOpened && minMaxNow < 0) {
				orders.add(new Order(market.getMarketName(), SELL, PERCENTAGE, 100, 0.0));
			}
		}
		return orders;
	}
}
