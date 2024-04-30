package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.actions.AccountActionException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;

public class Account {
	private final double commission;
	private final Map<MarketName, Position> positions = new HashMap<>();
	private double cash;

	public Account(double cash, double commission) {
		this.cash = cash;
		this.commission = commission;
	}


	// TODO remove Optional
	public Optional<Position> getPosition(MarketName market) {
		return ofNullable(positions.get(market));
	}

	public Map<MarketName, Position> getPositions() {
		return new HashMap<>(positions);
	}

	public void addPosition(MarketName market, double price, double quantity) {
		assertPositive(quantity, price);
		double value = quantity * price;
		double cashNeeded = value * (1 + commission);
		if (cashNeeded > cash) {
			throw new AccountActionException("Not enough cash");
		}
		Position position = positions.get(market);
		if (position != null) {
			position.add(new Quantity(quantity), price);
		} else {
			positions.put(market, new Position(new Quantity(quantity), price));
		}
		cash -= cashNeeded;
	}

	public void reducePosition(MarketName market, double price, double quantity) {
		assertPositive(quantity, price);
		Position position = positions.get(market);
		if (position == null) {
			throw new AccountActionException("No position found");
		}
		position.reduce(quantity);
		if (position.getQuantity() == 0) {
			positions.remove(market);
		}
		double value = quantity * price;
		double cashReceived = value * (1 - commission);
		cash += cashReceived;
	}

	public void addCash(double cash) {
		assertPositive(cash);
		this.cash += cash;
	}

	public void reduceCash(double cash) {
		assertPositive(cash);
		this.cash -= cash;
	}

	public double getCash() {
		return cash;
	}

	public double getNAV() {
		double nav = cash;
		for (Position position : positions.values()) {
			nav += position.getLastPrice() * position.getQuantity();
		}
		return nav;
	}

	public void updateLastKnown(TimeMarketDataSet data) {
		positions.forEach((market, position) -> {
			TimeMarketData marketData = data.get(market);
			if (marketData != null) {
				double close = marketData.getData(Layer.CLOSE, 0);
				position.updateLastPrice(close);
			}
		});

	}

	private void assertPositive(double... values) {
		for (int i = 0; i < values.length; i++) {
			if (values[i] < 0.0) {
				throw new IllegalArgumentException(format("Value %f at index %d is negative", values[i], i));
			}
		}
	}

}
