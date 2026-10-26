package com.alphatica.alis.trading.order;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.trading.account.Quantities;

import java.util.Locale;

import static java.lang.String.format;

/**
 * @param sizeValue percent, number of shares or cash value, depending on {@code size}
 */
public record Order(MarketName market, Direction direction, OrderSize size, double sizeValue,
					double priority) implements Comparable<Order> {
	@Override
	public int compareTo(Order o) {
		return Double.compare(priority, o.priority);
	}

	public String formatSize() {
		return switch (size) {
			case COUNT -> format("%s shares", Quantities.format(sizeValue));
			case PERCENTAGE -> format("%s %%", Quantities.format(sizeValue));
			case VALUE -> format(Locale.US, "%.2f value", sizeValue);
		};
	}
}
