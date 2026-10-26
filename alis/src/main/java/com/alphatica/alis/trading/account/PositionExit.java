package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.time.Time;

import static com.alphatica.alis.tools.java.NumberTools.assertPositive;

public class PositionExit {
	final Time time;
	final long units;
	final double price;

	/**
	 * @param quantity number of shares, rounded to {@link Quantities#MIN_QUANTITY}
	 */
	public PositionExit(Time time, double quantity, double price) {
		this.units = Quantities.toUnits(quantity);
		assertPositive(units);
		this.time = time;
		this.price = price;
	}

	double quantity() {
		return Quantities.toShares(units);
	}
}
