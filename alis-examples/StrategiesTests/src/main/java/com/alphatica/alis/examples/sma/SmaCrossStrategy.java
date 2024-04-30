package com.alphatica.alis.examples.sma;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.trend.Sma;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;

import java.util.ArrayList;
import java.util.List;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;
import static com.alphatica.alis.trading.order.Direction.LONG;
import static com.alphatica.alis.trading.order.Direction.EXIT;
import static com.alphatica.alis.trading.order.OrderSize.PROPORTION;

public class SmaCrossStrategy extends Strategy {
	private static final Sma shortSma = new Sma(50);
	private static final Sma longSma = new Sma(200);

	public List<Order> afterClose(TimeMarketDataSet data, Account account) {
		List<Order> orders = new ArrayList<>();
		for (TimeMarketData market : data.listMarkets(STOCKS)) {
			double longSmaV = longSma.calculate(market);
			double shortSmaV = shortSma.calculate(market);
			MarketName marketName = market.getMarketName();
			boolean positionOpened = account.getPosition(marketName) != null;
			if (!positionOpened && shortSmaV > longSmaV) {
				orders.add(new Order(marketName, LONG, PROPORTION, 1.0, 1.0));
			}
			if (positionOpened && shortSmaV < longSmaV) {
				orders.add(new Order(marketName, EXIT, PROPORTION, 1.0, 1.0));
			}
		}
		return orders;
	}
}
