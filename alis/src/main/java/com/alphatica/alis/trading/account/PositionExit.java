package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.time.Time;

import static com.alphatica.alis.tools.java.NumberTools.assertPositive;

public class PositionExit {
	final Time time;
	final int quantity;
	final double price;

	public PositionExit(Time time, int quantity, double price) {
		assertPositive(quantity, price);
		this.time = time;
		this.quantity = quantity;
		this.price = price;
	}
}
