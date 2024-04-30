package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.PositionStats;

import java.util.HashMap;
import java.util.Map;

import static com.alphatica.alis.tools.java.NumberTools.assertPositive;

public class Account {
	private final double commission;
	private final Map<MarketName, Position> positions = new HashMap<>();
	private double cash;
	private double cashBalance;
	private double maxCashBalance;
	private double currentDD;
	private double maxDD;

	private final PositionsHistory positionsHistory;

	public Account(double cash, double commission) {
		this.cash = cash;
		this.cashBalance = cash;
		this.maxCashBalance = cash;
		this.currentDD = 0;
		this.maxDD = 0;
		this.commission = commission;
		positionsHistory = new PositionsHistory();
	}


	public double getMaxDD() {
		return maxDD;
	}

	public double getCurrentDD() {
		return currentDD;
	}

	public Position getPosition(MarketName market) {
		return positions.get(market);
	}

	public double getPositionValue(MarketName marketName) {
		Position position = positions.get(marketName);
		if (position != null) {
			return position.getQuantity() * position.getEntryPrice();
		} else {
			return 0;
		}
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
			position.add(new PositionEntry(quantity, price));
		} else {
			positions.put(market, new Position(new PositionEntry(quantity, price)));
		}
		cash -= cashNeeded;
	}

	public void reducePosition(MarketName market, PositionExit exit) {
		Position position = positions.get(market);
		if (position == null) {
			throw new AccountActionException("No position found");
		}
		position.reduce(exit);
		double value = exit.price * exit.quantity;
		double cashReceived = value * (1 - commission);
		cash += cashReceived;
		if (position.getQuantity() == 0) {
			Position removed = positions.remove(market);
			addToHistory(market, removed);
		}
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

	public PositionsHistory getPositionsHistory() {
		return positionsHistory;
	}

	private void addToHistory(MarketName marketName, Position removed) {
		PositionStats stats = removed.getStats();
		cashBalance += stats.profitValue();
		if (cashBalance > maxCashBalance) {
			maxCashBalance = cashBalance;
		}
		currentDD = (cashBalance / maxCashBalance - 1 ) * 100;
		if (currentDD < maxDD) {
			maxDD = currentDD;
		}
		double profitAsNAVPercent = (stats.profitValue() / getNAV() - 1.0) * 100;
		PositionResult positionResult = new PositionResult(stats.profitValue(), stats.profitPercent(), profitAsNAVPercent);
		positionsHistory.add(marketName, positionResult);
	}

}
