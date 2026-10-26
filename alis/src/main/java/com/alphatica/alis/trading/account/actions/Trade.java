package com.alphatica.alis.trading.account.actions;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.PositionEntry;
import com.alphatica.alis.trading.account.PositionExit;
import com.alphatica.alis.trading.account.Quantities;
import com.alphatica.alis.trading.order.Direction;

import java.util.Locale;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

/**
 * @param quantity number of shares, possibly fractional; rounded to {@link Quantities#MIN_QUANTITY}
 */
public record Trade(MarketName marketName, Direction direction, double price, double quantity, double commission) implements AccountActionType {

	public Trade {
		quantity = Quantities.normalize(quantity);
	}

	@Override
	public void doOnAccount(Time time, Account account) throws AccountActionException {
		if (requireNonNull(direction) == Direction.BUY) {
			account.addPosition(marketName, new PositionEntry(time, quantity, price), commission);
		} else if (direction == Direction.SELL) {
			account.reducePosition(marketName, new PositionExit(time, quantity, price), commission);
		}
	}

	@Override
	public String toCsv() {
		return format(Locale.US, "Trade,%s,%s,%.2f,%s,%.2f", marketName, direction, price, Quantities.format(quantity), commission);
	}

	@Override
	public String toString() {
		return "Executing trade: " + direction + " " + Quantities.format(quantity) + " x " + marketName + " @ " + price;
	}
}
