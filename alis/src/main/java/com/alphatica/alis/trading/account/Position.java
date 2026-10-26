package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.actions.AccountActionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Position {
	private final List<PositionEntry> entries;
	private final List<PositionExit> exits;
	private final MarketName marketName;
	private double entryCommissions;
	private double exitCommissions;
	private double lastClose;
	private int tradeLength;
	// Averaged history of this position cycle (see PositionHistoryRecord)
	private long closedUnits;
	private double closedEntryValue;
	private double closedExitValue;
	private double firstExitPrice;
	private Time lastExitTime;
	private double lowestLow;
	private double highestHigh;
	private double closedLowestLow;
	private double closedHighestHigh;
	private int closedTradeLength;

	public Position(MarketName marketName, PositionEntry entry) {
		this(marketName, entry, 0);
	}

	Position(MarketName marketName, PositionEntry entry, double commission) {
		this.entries = new ArrayList<>();
		this.exits = new ArrayList<>();
		this.entries.add(entry);
		this.entryCommissions = commission;
		this.lastClose = entry.price;
		this.marketName = marketName;
		this.tradeLength = 0;
		this.lowestLow = entry.price;
		this.highestHigh = entry.price;
	}

	public double getEntryPrice() {
		double v = entries.stream().map(p -> p.quantity() * p.price).reduce(0.0, Double::sum);
		return v / getQuantity();
	}

	/**
	 * Entry value (quantity * entry price, without commissions) of the still open part of this position.
	 */
	public double getOpenEntryValue() {
		return entries.stream().mapToDouble(e -> e.quantity() * e.price).sum();
	}

	/**
	 * Number of shares held, possibly fractional.
	 */
	public double getQuantity() {
		return Quantities.toShares(getUnits());
	}

	long getUnits() {
		long units = 0;
		for (PositionEntry entry : entries) {
			units += entry.units;
		}
		return units;
	}

	public void add(PositionEntry entry) {
		add(entry, 0);
	}

	void add(PositionEntry entry, double commission) {
		this.entries.add(entry);
		entryCommissions += commission;
	}

	public List<PositionPricesRecord> reduce(PositionExit exit) throws AccountActionException {
		return reduce(exit, 0);
	}

	List<PositionPricesRecord> reduce(PositionExit exit, double commission) throws AccountActionException {
		if (exit.units > getUnits()) {
			throw new AccountActionException("Not enough quantity to reduce ");
		}
		List<PositionPricesRecord> positionPricesRecords = new ArrayList<>();
		exits.add(exit);
		exitCommissions += commission;
		long toRemove = exit.units;
		int checkIndex = 0;
		while (toRemove > 0 && checkIndex < entries.size()) {
			PositionEntry entry = entries.get(checkIndex);
			checkIndex++;
			if (entry.units == 0) {
				continue;
			}
			long removing = Math.min(toRemove, entry.units);
			entry.units -= removing;
			toRemove -= removing;
			double removedQuantity = Quantities.toShares(removing);
			closedEntryValue += removedQuantity * entry.price;
			positionPricesRecords.add(new PositionPricesRecord(marketName, entry.time, exit.time, entry.price, exit.price, removedQuantity, entry.lowestLow
					, entry.highestHigh));
		}
		recordClosedPart(exit);
		return positionPricesRecords;
	}

	public List<PositionPricesRecord> getOpenPositionsPricesRecords() {
		List<PositionPricesRecord> positionPricesRecords = new ArrayList<>();
		for (PositionEntry entry : entries) {
			if (entry.units > 0) {
				positionPricesRecords.add(new PositionPricesRecord(marketName, entry.time, null, entry.price, lastClose, entry.quantity(),
						entry.lowestLow, entry.highestHigh));
			}
		}
		return positionPricesRecords;
	}

	public void updatePrices(double close, double high, double low) {
		lastClose = close;
		tradeLength++;
		if (high > highestHigh) {
			highestHigh = high;
		}
		if (low < lowestLow) {
			lowestLow = low;
		}
		for (PositionEntry entry : entries) {
			if (entry.units > 0) {
				entry.sessions++;
				if (high > entry.highestHigh) {
					entry.highestHigh = high;
				}
				if (low < entry.lowestLow) {
					entry.lowestLow = low;
				}
			}
		}
	}

	/**
	 * Closed part of this position: from the first buy to the last sell. Empty if nothing was sold yet.
	 */
	public Optional<PositionHistoryRecord> getClosedHistoryRecord() {
		if (closedUnits == 0) {
			return Optional.empty();
		}
		PositionEntry firstEntry = entries.getFirst();
		double closedQuantity = Quantities.toShares(closedUnits);
		double entryCommission = entryCommissions * closedQuantity / totalBoughtQuantity();
		return Optional.of(new PositionHistoryRecord(marketName, firstEntry.time, lastExitTime, closedEntryValue / closedQuantity,
				closedExitValue / closedQuantity, closedQuantity, entryCommission, exitCommissions, closedTradeLength,
				closedLowestLow / firstEntry.price, firstExitPrice / closedHighestHigh, false));
	}

	/**
	 * Open part of this position: from the oldest remaining lot to the last close. Empty if position is fully closed.
	 */
	public Optional<PositionHistoryRecord> getOpenHistoryRecord() {
		long units = getUnits();
		if (units == 0) {
			return Optional.empty();
		}
		double quantity = Quantities.toShares(units);
		PositionEntry oldest = entries.stream().filter(e -> e.units > 0).findFirst().orElseThrow();
		double entryCommission = entryCommissions * quantity / totalBoughtQuantity();
		return Optional.of(new PositionHistoryRecord(marketName, oldest.time, null, getEntryPrice(), lastClose, quantity, entryCommission,
				0, oldest.sessions, oldest.lowestLow / oldest.price, lastClose / oldest.highestHigh, true));
	}

	public List<PositionHistoryRecord> getHistoryRecords() {
		List<PositionHistoryRecord> records = new ArrayList<>(2);
		getClosedHistoryRecord().ifPresent(records::add);
		getOpenHistoryRecord().ifPresent(records::add);
		return records;
	}

	private void recordClosedPart(PositionExit exit) {
		if (closedUnits == 0) {
			firstExitPrice = exit.price;
		}
		closedUnits += exit.units;
		closedExitValue += exit.quantity() * exit.price;
		lastExitTime = exit.time;
		closedLowestLow = lowestLow;
		closedHighestHigh = highestHigh;
		closedTradeLength = tradeLength;
	}

	private double totalBoughtQuantity() {
		return entries.stream().mapToDouble(e -> Quantities.toShares(e.initialUnits)).sum();
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
