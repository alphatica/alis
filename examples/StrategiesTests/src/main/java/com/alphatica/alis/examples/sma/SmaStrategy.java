package com.alphatica.alis.examples.sma;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.trend.Sma;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;
import com.alphatica.alis.trading.optimizer.params.IntParam;

import java.util.ArrayList;
import java.util.List;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;
import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;
import static com.alphatica.alis.trading.order.OrderSize.PERCENTAGE;

public class SmaStrategy extends Strategy {

	@IntParam(start = 5, step = 1, end = 350)
	int smaLength;
	private Sma sma;

	public SmaStrategy(int length) {
		smaLength = length;
		sma = new Sma(length);
	}

	@Override
	public void paramsChanged() {
		sma = new Sma(smaLength);
	}

	@Override
	public List<Order> afterClose(TimeMarketDataSet data, Account account) {
		List<Order> orders = new ArrayList<>();
		for (TimeMarketData market : data.listUpToDateMarkets(STOCKS)) {
			double currentSma = sma.calculate(market);
			MarketName marketName = market.getMarketName();
			boolean positionOpened = account.getPosition(marketName) != null;
			if (!positionOpened && market.getData(CLOSE, 0) > currentSma) {
				orders.add(new Order(marketName, BUY, PERCENTAGE, 5, 1.0));
			}
			if (positionOpened && market.getData(CLOSE, 0) < currentSma) {
				orders.add(new Order(marketName, SELL, PERCENTAGE, 100, 1.0));
			}
		}
		return orders;
	}
}
