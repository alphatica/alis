package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.PositionStats;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.alphatica.alis.tools.java.NumberTools.percentChange;

public class Position {
	private final List<PositionEntry> entries;
	private final List<PositionExit> exits;
	private final MarketName marketName;
	private double lastClose;

	public Position(MarketName marketName, PositionEntry entry) {
		this.entries = new ArrayList<>();
		this.exits = new ArrayList<>();
		this.entries.add(entry);
		this.lastClose = entry.price;
		this.marketName = marketName;
	}

	public double getEntryPrice() {
		double v = entries.stream().map(p -> p.quantity * p.price).reduce(0.0, Double::sum);
		int q = entries.stream().map(p -> p.quantity).reduce(0, Integer::sum);
		return v / q;
	}

	public PositionStats getStats() {
		double entryValue = entries.stream().map(p -> p.initialQuantity * p.price).reduce(0.0, Double::sum);
		double exitValue = exits.stream().map(p -> p.quantity * p.price).reduce(0.0, Double::sum);
		double profitValue = exitValue - entryValue;
		return new PositionStats(profitValue, percentChange(entryValue, exitValue));
	}

	public int getQuantity() {
		return entries.stream().map(q -> q.quantity).reduce(0, Integer::sum);
	}

	public void add(PositionEntry entry) {
		this.entries.add(entry);
	}

	public List<PositionPricesRecord> reduce(PositionExit exit) throws AccountActionException {
		if (exit.quantity > getQuantity()) {
			throw new AccountActionException("Not enough quantity to reduce ");
		}
		List<PositionPricesRecord> positionPricesRecords = new ArrayList<>();
		exits.add(exit);
		int toRemove = exit.quantity;
		int checkIndex = 0;
		while (toRemove > 0 && checkIndex < entries.size()) {
			PositionEntry entry = entries.get(checkIndex);
			checkIndex++;
			if (entry.quantity == 0) {
				continue;
			}
			int removing = Math.min(toRemove, entry.quantity);
			entry.quantity -= removing;
			toRemove -= removing;
			positionPricesRecords.add(new PositionPricesRecord(marketName, entry.time, exit.time, entry.price, exit.price, removing, entry.lowestLow
					, entry.highestHigh));
		}
		return positionPricesRecords;
	}

	public List<PositionPricesRecord> getOpenPositionsPricesRecords() {
		List<PositionPricesRecord> positionPricesRecords = new ArrayList<>();
		for (PositionEntry entry : entries) {
			if (entry.quantity > 0) {
				positionPricesRecords.add(new PositionPricesRecord(marketName, entry.time, null, entry.price, lastClose, entry.quantity,
						entry.lowestLow, entry.highestHigh));
			}
		}
		return positionPricesRecords;
	}

	public void updatePrices(double close, double high, double low) {
		lastClose = close;
		for (PositionEntry entry : entries) {
			if (entry.quantity > 0) {
				if (high > entry.highestHigh) {
					entry.highestHigh = high;
				}
				if (low < entry.lowestLow) {
					entry.lowestLow = low;
				}
			}
		}
	}

	public double getLastClose() {
		return lastClose;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		Position position = (Position) o;
		return Double.compare(lastClose, position.lastClose) == 0 && Objects.equals(entries, position.entries) && Objects.equals(exits, position.exits) && Objects.equals(marketName, position.marketName);
	}

	@Override
	public int hashCode() {
		return Objects.hash(entries, exits, marketName, lastClose);
	}
}
