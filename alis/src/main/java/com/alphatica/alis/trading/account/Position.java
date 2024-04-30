package com.alphatica.alis.trading.account;

import com.alphatica.alis.trading.account.actions.AccountActionException;

public class Position {
	private final Quantity quantity;
	private final double entryPrice;
	private double lastPrice;

	public Position(Quantity quantity, double entryPrice) {
		this.quantity = quantity;
		this.entryPrice = entryPrice;
	}

	public double getEntryPrice() {
		return entryPrice;
	}

	public double getQuantity() {
		return quantity.quantity;
	}

	// TODO use price
	public void add(Quantity quantity, double entryPrice) {
		this.quantity.quantity += quantity.quantity;
	}

	public void reduce(double quantity) {
		if (quantity > this.quantity.quantity) {
			throw new AccountActionException("Not enough quantity to reduce ");
		}
		this.quantity.quantity -= quantity;
	}

	public void updateLastPrice(double price) {
		lastPrice = price;
	}

	public double getLastPrice() {
		return lastPrice;
	}
}
