package com.alphatica.alis.examples.rateofchange;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.data.FloatArraySlice;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Direction;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.order.OrderSize;
import com.alphatica.alis.trading.strategy.Strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;

public class RateOfChangeStrategy extends Strategy {
	private final Set<MarketName> allowedMarkets;
	private final int buyPeriod;
	private final int sellPeriod;

	private final double buyChange;
	private final double sellChange;
	private final int requiredLength;

	public RateOfChangeStrategy(Set<MarketName> allowedMarkets, int buyPeriod, int sellPeriod, double buyChange, double sellChange) {
		this.allowedMarkets = allowedMarkets;
		this.buyPeriod = buyPeriod;
		this.sellPeriod = sellPeriod;
		this.buyChange = buyChange;
		this.sellChange = sellChange;
		requiredLength = Math.max(buyPeriod, sellPeriod);
	}

	@Override
	public List<Order> afterClose(TimeMarketDataSet data, Account account) {
		List<Order> orders = new ArrayList<>(1024);
		for (TimeMarketData marketData : data.listUpToDateMarkets(STOCKS)) {
			if (!allowedMarkets.contains(marketData.getMarketName())) {
				continue;
			}
			orderFor(marketData, account).ifPresent(orders::add);
		}
		return orders;
	}

	private Optional<Order> orderFor(TimeMarketData marketData, Account account) {
		FloatArraySlice closes = marketData.getLayer(Layer.CLOSE);
		if (closes.size() <= requiredLength) {
			return Optional.empty();
		}
		boolean buySignal = change(closes, buyPeriod) > buyChange;
		boolean sellSignal = change(closes, sellPeriod) < sellChange;
		boolean hasPosition = account.getPosition(marketData.getMarketName()) != null;
		if (hasPosition && sellSignal && !buySignal) {
			return Optional.of(new Order(marketData.getMarketName(), Direction.SELL, OrderSize.PERCENTAGE, 100, 0.0));
		}
		if (!hasPosition && buySignal && !sellSignal) {
			return Optional.of(new Order(marketData.getMarketName(), Direction.BUY, OrderSize.PERCENTAGE, 5, buyChange));
		}
		return Optional.empty();
	}

	private static double change(FloatArraySlice closes, int period) {
		return (closes.get(0) / closes.get(period) - 1) * 100;
	}
}
