package com.alphatica.alis.examples.sma;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.trend.MinMax;
import com.alphatica.alis.indicators.trend.Sma;
import com.alphatica.alis.tools.data.DoubleArraySlice;
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

public class SmaWithMinMax implements Strategy {
	private final MinMax minMax;
	private final Sma sma;

	public SmaWithMinMax(MinMax minMax, Sma sma) {
		this.minMax = minMax;
		this.sma = sma;
	}

	@Override
	public List<Order> afterClose(TimeMarketDataSet data, Account account) {
		System.out.println("_____________________________________________");
		System.out.println("Time " + data.getTime());
		List<Order> orders = new ArrayList<>();
		for (TimeMarketData market : data.listMarkets(STOCKS)) {
			DoubleArraySlice closes = market.getLayer(CLOSE);
			if (closes.size() <= 250) {
				continue;
			}
			MarketName marketName = market.getMarketName();
			boolean positionOpened = account.getPosition(marketName).isPresent();
			if (!positionOpened && entryConditions(market)) {
				double priority = closes.get(0) / closes.get(250);
				orders.add(new Order(marketName, BUY, PROPORTION, 0.05, priority));
			}
			if (positionOpened && exitConditions(market)) {
				orders.add(new Order(marketName, SELL, PROPORTION, 1.0, 1.0));
			}
		}
		return orders;
	}

	private boolean exitConditions(TimeMarketData market) {
		return minMax.calculate(market) < 0 || market.getData(CLOSE, 0) < sma.calculate(market);
	}

	private boolean entryConditions(TimeMarketData market) {
		return minMax.calculate(market) > 0 && market.getData(CLOSE, 0) > sma.calculate(market);
	}
}
