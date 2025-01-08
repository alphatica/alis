package com.alphatica.alis.trading.account.actions;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.PositionEntry;
import com.alphatica.alis.trading.account.PositionExit;
import com.alphatica.alis.trading.order.Direction;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;

public record Trade(MarketName marketName, Direction direction, double price, int quantity, double commission) implements AccountActionType {
	@Override
	public void doOnAccount(Time time, Account account) throws AccountActionException {
		if (Objects.requireNonNull(direction) == Direction.BUY) {
			account.addPosition(marketName, new PositionEntry(time, quantity, price), commission);
		} else if (direction == Direction.SELL) {
			account.reducePosition(marketName, new PositionExit(time, quantity, price), commission);
		}
	}

	@Override
	public String toCsv() {
		return format(Locale.US, "Trade,%s,%s,%.2f,%d,%.2f", marketName, direction, price, quantity, commission);
	}

	@Override
	public String toString() {
		return "Executing trade: " + direction + " " + quantity + " x " + marketName + " @ " + price;
	}

	@Override
	public Map<String, String> toAttributes() {
		Map<String, String> row = new HashMap<>();
		double value = quantity * price;
		if (direction == Direction.BUY) {
			value += commission;
		} else if (direction == Direction.SELL) {
			value -= commission;
		}
		row.put("Value", format("%.2f", value));
		row.put("Type", direction.toString());
		row.put("Market", marketName.name());
		row.put("Price", format("%.2f", price));
		row.put("Quantity", format("%d", quantity));
		row.put("Commission", format("%.2f", commission));
		return row;
	}
}
