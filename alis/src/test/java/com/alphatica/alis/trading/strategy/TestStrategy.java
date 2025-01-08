package com.alphatica.alis.trading.strategy;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Direction;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.order.OrderSize;

import java.util.List;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;
import static com.alphatica.alis.tools.java.CollectionsTools.arrayList;

class TestStrategy extends Strategy {
	/*
	Buy stock at 100 for half of the money
	Sell at 200.
	This should give 50% profit - commissions.
	 */
	@Override
	public List<Order> afterClose(TimeMarketDataSet data, Account account) {
		for (TimeMarketData marketData : data.listMarkets(STOCKS)) {
			if (data.getTime().equals(new Time(99))) {
				return arrayList(new Order(marketData.getMarketName(), Direction.BUY, OrderSize.PERCENTAGE, 50, 1.0));
			}
			if (data.getTime().equals(new Time(199))) {
				return arrayList(new Order(marketData.getMarketName(), Direction.SELL, OrderSize.PERCENTAGE, 100, 1.0));
			}
		}
		return arrayList();
	}
}
