package com.alphatica.alis.trading.account;

import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.PositionStats;

import java.util.ArrayList;
import java.util.List;

import static com.alphatica.alis.tools.java.NumberTools.percentChange;

public class Position {
	private final List<PositionEntry> entries;
	private final List<PositionExit> exits;
	private double lastPrice;

	public Position(PositionEntry entry) {
		this.entries = new ArrayList<>();
		this.exits = new ArrayList<>();
		this.entries.add(entry);
	}

	public double getEntryPrice() {
		double v = entries.stream().map(p -> p.quantity * p.price).reduce(0.0, Double::sum);
		double q = entries.stream().map(p -> p.quantity).reduce(0.0, Double::sum);
		return v / q;
	}

	public PositionStats getStats() {
		double entryValue = entries.stream().map(p -> p.initialQuantity * p.price).reduce(0.0, Double::sum);
		double exitValue = exits.stream().map(p -> p.quantity * p.price).reduce(0.0, Double::sum);
		double profitValue = exitValue - entryValue;
		return new PositionStats(profitValue, percentChange(entryValue, exitValue));
	}

	public double getQuantity() {
		return entries.stream().map(q -> q.quantity).reduce(0.0, Double::sum);
	}

	public void add(PositionEntry entry) {
		this.entries.addFirst(entry);
	}

	public void reduce(PositionExit exit) {
		if (exit.quantity > getQuantity()) {
			throw new AccountActionException("Not enough quantity to reduce ");
		}
		exits.add(exit);
		double toRemove = exit.quantity;
		int checkIndex = entries.size() - 1;
		while (toRemove > 0 && checkIndex >= 0) {
			PositionEntry last = entries.get(checkIndex);
			double removing = Math.min(toRemove, last.quantity);
			last.quantity -= removing;
			toRemove -= removing;
			checkIndex--;
		}
	}

	public void updateLastPrice(double price) {
		lastPrice = price;
	}

	public double getLastPrice() {
		return lastPrice;
	}
}
