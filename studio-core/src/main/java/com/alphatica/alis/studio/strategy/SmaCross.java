package com.alphatica.alis.studio.strategy;

import com.alphatica.alis.data.FloatArraySlice;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.trend.Sma;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.optimizer.params.IntParam;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;

import java.util.ArrayList;
import java.util.List;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;
import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;
import static com.alphatica.alis.trading.order.OrderSize.PERCENTAGE;

public class SmaCross extends Strategy {

	@IntParam(start = 120, step = 5, end = 320)
	int longTermSmaLen;

	@IntParam(start = 10, step = 1, end = 60)
	int shortTermSmaLen;

	@IntParam(start = 60, step = 5, end = 250)
	int priorityCheckLen;

	@IntParam(start = 3, step = 1, end = 10)
	int shortFilterLen;

	private Sma longTermSma;
	private Sma shortTermSma;
	private Sma shortFilterSma;

	public SmaCross() {
		longTermSmaLen = 200;
		shortTermSmaLen = 50;
		priorityCheckLen = 250;
		shortFilterLen = 5;
		paramsChanged();
	}

	@Override
	public void paramsChanged() {
		longTermSma = new Sma(longTermSmaLen);
		shortTermSma = new Sma(shortTermSmaLen);
		shortFilterSma = new Sma(shortFilterLen);
	}

	@Override
	public List<Order> afterClose(TimeMarketDataSet data, Account account) {
		List<Order> orders = new ArrayList<>();
		List<TimeMarketData> markets = data.listUpToDateMarkets(STOCKS);
		for (TimeMarketData marketData : markets) {
			if (account.getPosition(marketData.getMarketName()) != null) {
				checkSell(marketData, orders);
			}
		}
		for (TimeMarketData marketData : markets) {
			if (account.getPosition(marketData.getMarketName()) == null) {
				checkBuy(marketData, orders);
			}
		}
		return orders;
	}

	private void checkSell(TimeMarketData marketData, List<Order> orders) {
		float close = marketData.getLayer(CLOSE).get(0);
		float longSma = longTermSma.calculate(marketData);
		float shortSma = shortTermSma.calculate(marketData);
		if (shortSma < longSma || close < longSma) {
			orders.add(new Order(marketData.getMarketName(), SELL, PERCENTAGE, 100, 0.0));
		}
	}

	private void checkBuy(TimeMarketData marketData, List<Order> orders) {
		FloatArraySlice closes = marketData.getLayer(CLOSE);
		float longSma = longTermSma.calculate(marketData);
		float shortSma = shortTermSma.calculate(marketData);
		if (shortSma > longSma && closes.get(0) > shortFilterSma.calculate(marketData)) {
			orders.add(new Order(marketData.getMarketName(), BUY, PERCENTAGE, 5, orderPriority(closes)));
		}
	}

	private double orderPriority(FloatArraySlice closes) {
		int back = Math.min(priorityCheckLen, closes.size() - 1);
		return closes.get(0) / closes.get(back);
	}
}
