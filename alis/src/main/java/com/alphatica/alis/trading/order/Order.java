package com.alphatica.alis.trading.order;

import com.alphatica.alis.data.market.MarketName;

import static java.lang.String.format;

public record Order(MarketName market, Direction direction, OrderSize size, int sizeValue,
					double priority) implements Comparable<Order> {
	@Override
	public int compareTo(Order o) {
		return Double.compare(priority, o.priority);
	}

	public String formatSize() {
		return switch (size) {
			case COUNT -> format("%d shares", sizeValue);
			case PERCENTAGE -> format("%d %%", sizeValue);
		};
	}
}
