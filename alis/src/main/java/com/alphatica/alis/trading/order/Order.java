package com.alphatica.alis.trading.order;

import com.alphatica.alis.data.market.MarketName;

public record Order(MarketName market, Direction direction, OrderSize size, double sizeValue,
					double priority) implements Comparable<Order> {
	@Override
	public int compareTo(Order o) {
		return Double.compare(priority, o.priority);
	}
}
