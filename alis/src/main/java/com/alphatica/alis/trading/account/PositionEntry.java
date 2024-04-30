package com.alphatica.alis.trading.account;

import static com.alphatica.alis.tools.java.NumberTools.assertPositive;

public class PositionEntry {
	final double initialQuantity;
	double quantity;
	double price;

	public PositionEntry(double quantity, double price) {
		assertPositive(quantity, price);
		this.quantity = quantity;
		this.initialQuantity = quantity;
		this.price = price;
	}
}
