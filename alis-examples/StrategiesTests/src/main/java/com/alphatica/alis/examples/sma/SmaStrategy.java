package com.alphatica.alis.examples.sma;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.indicators.trend.Sma;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;
import com.alphatica.alis.trading.strategy.params.IntParam;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;
import static com.alphatica.alis.trading.order.Direction.LONG;
import static com.alphatica.alis.trading.order.Direction.EXIT;
import static com.alphatica.alis.trading.order.OrderSize.PROPORTION;

public class SmaStrategy extends Strategy {

	private Sma sma;
	int smaLength;

	public SmaStrategy(int length) {
		params = List.of(
				new IntParam("sma_length", () -> ThreadLocalRandom.current().nextInt(5, 350), (i) -> smaLength = i, () -> smaLength)
		);
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
		for (TimeMarketData market : data.listMarkets(STOCKS)) {
			double currentSma = sma.calculate(market);
			MarketName marketName = market.getMarketName();
			boolean positionOpened = account.getPosition(marketName) != null;
			if (!positionOpened && market.getData(CLOSE, 0) > currentSma) {
				orders.add(new Order(marketName, LONG, PROPORTION, 1.0, 1.0));
			}
			if (positionOpened && market.getData(CLOSE, 0) < currentSma) {
				orders.add(new Order(marketName, EXIT, PROPORTION, 1.0, 1.0));
			}
		}
		return orders;
	}
}
