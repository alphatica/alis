package com.alphatica.alis.studio.strategy;

import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Direction;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.order.OrderSize;
import com.alphatica.alis.trading.strategy.Strategy;
import com.alphatica.alis.trading.strategy.params.DoubleParam;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;

public class RandomStrategy extends Strategy {
	@DoubleParam(start = 0.0, step = 0.0001, end = 0.5)
	private double tradeProbability = 0.005;

	public void setTradeProbability(double tradeProbability) {
		this.tradeProbability = tradeProbability;
	}

	@Override
	public List<Order> afterClose(TimeMarketDataSet timeMarketDataSet, Account account) {
		List<Order> orders = new ArrayList<>();
		timeMarketDataSet.listMarkets(STOCKS).forEach(market -> {
			boolean shouldTrade = ThreadLocalRandom.current().nextDouble() < tradeProbability;
			if (shouldTrade) {
				if (account.getPosition(market.getMarketName()) != null) {
					orders.add(new Order(market.getMarketName(), Direction.SELL, OrderSize.PERCENTAGE, 100, 1));
				} else {
					orders.add(new Order(market.getMarketName(), Direction.BUY, OrderSize.PERCENTAGE, 5, 1));
				}
			}
		});
		return orders;
	}
}
