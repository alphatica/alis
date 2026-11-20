package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;

import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.stream.Collectors;

final class ReducedMarketDataFactory {

	MarketData create(MarketData marketData) {
		Set<MarketName> excluded = marketData.listMarkets(excludeMarket()).stream()
				.map(Market::getName)
				.collect(Collectors.toSet());
		return new ReducedMarketData(marketData, excluded);
	}

	private static Predicate<Market> excludeMarket() {
		return market -> market.getType() == MarketType.STOCK && ThreadLocalRandom.current().nextDouble() <= 0.5;
	}
}
