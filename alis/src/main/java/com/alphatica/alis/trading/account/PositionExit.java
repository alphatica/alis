package com.alphatica.alis.trading.account;

import static com.alphatica.alis.tools.java.NumberTools.assertPositive;

public class PositionExit {
	final double quantity;
	final double price;

	public PositionExit(double quantity, double price) {
		assertPositive(quantity, price);
		this.quantity = quantity;
		this.price = price;
	}
}
