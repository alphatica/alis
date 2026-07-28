package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.data.StandardMarketData;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class ReducedMarketDataFactory {

	MarketData create(MarketData marketData) {
		Map<MarketName, Market> markets = marketData.listMarkets(acceptMarket()).stream()
				.collect(Collectors.toMap(Market::getName, market -> market));
		StandardMarketData reducedMarketData = new StandardMarketData();
		reducedMarketData.addMarkets(markets);
		return reducedMarketData;
	}

	private static Predicate<Market> acceptMarket() {
		return market -> market.getType() != MarketType.STOCK || ThreadLocalRandom.current().nextDouble() > 0.5;
	}
}
