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

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;
import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;
import static com.alphatica.alis.trading.order.OrderSize.PROPORTION;

public class SmaStrategy implements Strategy {

	private final Sma sma;

	public SmaStrategy(int length) {
		this.sma = new Sma(length);
	}

	@Override
	public List<Order> afterClose(TimeMarketDataSet data, Account account) {
		List<Order> orders = new ArrayList<>();
		for (TimeMarketData market : data.listMarkets(STOCKS)) {
			double currentSma = sma.calculate(market);
			MarketName marketName = market.getMarketName();
			boolean positionOpened = account.getPosition(marketName).isPresent();
			if (!positionOpened && market.getData(CLOSE, 0) > currentSma) {
				orders.add(new Order(marketName, BUY, PROPORTION, 1.0, 1.0));
			}
			if (positionOpened && market.getData(CLOSE, 0) < currentSma) {
				orders.add(new Order(marketName, SELL, PROPORTION, 1.0, 1.0));
			}
		}
		return orders;
	}
}
