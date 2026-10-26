package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.time.Time;

import static com.alphatica.alis.tools.java.NumberTools.assertPositive;

public class PositionEntry {
	final long initialUnits;
	final double price;
	final Time time;
	long units;
	double highestHigh;
	double lowestLow;
	int sessions;

	/**
	 * @param quantity number of shares, rounded to {@link Quantities#MIN_QUANTITY}
	 */
	public PositionEntry(Time time, double quantity, double price) {
		this(Quantities.toUnits(quantity), time, price);
	}

	private PositionEntry(long units, Time time, double price) {
		assertPositive(units, price);
		this.units = units;
		this.initialUnits = units;
		this.price = price;
		this.time = time;
		// TODO remove from this class
		this.lowestLow = price;
		this.highestHigh = price;
	}

	static PositionEntry ofUnits(Time time, long units, double price) {
		return new PositionEntry(units, time, price);
	}

	double quantity() {
		return Quantities.toShares(units);
	}
}
