package com.alphatica.alis.studio.strategy;

import com.alphatica.alis.condition.HighestClose;
import com.alphatica.alis.condition.LowestClose;
import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.FloatArraySlice;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;
import com.alphatica.alis.trading.optimizer.params.BoolParam;
import com.alphatica.alis.trading.optimizer.params.IntParam;

import java.util.ArrayList;
import java.util.List;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;
import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;
import static com.alphatica.alis.trading.order.OrderSize.PERCENTAGE;

public class DonchianChannel extends Strategy {

//	@IntParam(start = 5, step = 1, end = 500)
	int sellLength;

	@BoolParam
	boolean highestGrowthFirst;

	@IntParam(start = 5, step = 5, end = 20)
	int buyLength;

	private HighestClose highestClose;
	private LowestClose lowestClose;

	public DonchianChannel() {
//		buyLength = 6;
//		sellLength = 114;
//		highestGrowthFirst = false;

		sellLength = 249;
		highestGrowthFirst = false;
		buyLength = 9;
		paramsChanged();
	}

	@Override
	public void paramsChanged() {
		this.highestClose = new HighestClose(buyLength);
		this.lowestClose = new LowestClose(sellLength);
	}

	@Override
	public List<Order> afterClose(TimeMarketDataSet allData, Account account) {
		List<Order> orders = new ArrayList<>();
		for (TimeMarketData marketData : allData.listUpToDateMarkets(STOCKS)) {
			checkStock(account, marketData, allData, orders);
		}
		return orders;
	}

	private void checkStock(Account account, TimeMarketData marketData, TimeMarketDataSet allData, List<Order> orders) {
		boolean hasPosition = account.getPosition(marketData.getMarketName()) != null;
		if (hasPosition && lowestClose.matches(marketData, allData)) {
			orders.add(new Order(marketData.getMarketName(), SELL, PERCENTAGE, 100, 0.0));
		}
		if (!hasPosition && highestClose.matches(marketData, allData)) {
			double priority = orderPriority(marketData.getLayer(Layer.CLOSE));
			orders.add(new Order(marketData.getMarketName(), BUY, PERCENTAGE, 5, priority));
		}
	}

	private double orderPriority(FloatArraySlice closes) {
		double priority;
		if (closes.size() > 250) {
			priority = closes.get(0) / closes.get(250);
		} else {
			priority = closes.get(0) / closes.get(closes.size() - 1);
		}
		if (!highestGrowthFirst) {
			priority = 1 / priority;
		}
		return priority;
	}

}
