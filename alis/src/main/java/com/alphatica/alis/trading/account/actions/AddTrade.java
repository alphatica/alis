package com.alphatica.alis.trading.account.actions;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Direction;

import java.util.Objects;

public record AddTrade(MarketName marketName, Direction direction, double price, double quantity) implements AccountActionType {
	@Override
	public void doOnAccount(Account account) {
		if (Objects.requireNonNull(direction) == Direction.BUY) {
			account.addPosition(marketName, price, quantity);
		} else if (direction == Direction.SELL) {
			account.reducePosition(marketName, price, quantity);
		}
	}

	@Override
	public String toString() {
		return "Executing trade: " + direction + " " + quantity + " x " + marketName + " @ " + price;
	}
}
