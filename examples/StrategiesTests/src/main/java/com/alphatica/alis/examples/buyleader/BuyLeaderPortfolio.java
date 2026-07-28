package com.alphatica.alis.examples.buyleader;

import com.alphatica.alis.data.market.MarketName;

import java.util.HashMap;
import java.util.Map;

final class BuyLeaderPortfolio {

	private final Map<MarketName, Position> positions = new HashMap<>();
	private double spent;

	void updatePrice(MarketName market, double price) {
		positions.computeIfAbsent(market, ignored -> new Position()).lastPrice = price;
	}

	void buy(MarketName market, double amount, double price) {
		Position position = positions.computeIfAbsent(market, ignored -> new Position());
		position.count += amount / price;
		position.lastPrice = price;
		spent += amount;
	}

	double spent() {
		return spent;
	}

	double totalValue() {
		return positions.values().stream().mapToDouble(position -> position.count * position.lastPrice).sum();
	}

	private static final class Position {
		private double count;
		private double lastPrice;
	}
}
