package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.time.Time;

import static com.alphatica.alis.tools.java.NumberTools.assertPositive;

public class PositionEntry {
	final double initialQuantity;
	final double price;
	final Time time;
	int quantity;
	double highestHigh;
	double lowestLow;

	public PositionEntry(Time time, int quantity, double price) {
		assertPositive(quantity, price);
		this.quantity = quantity;
		this.initialQuantity = quantity;
		this.price = price;
		this.time = time;
		// TODO remove from this class
		this.lowestLow = price;
		this.highestHigh = price;
	}
}
