package com.alphatica.alis.trading.order;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.tools.java.MarketAttributes;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

public record Order(MarketName market, Direction direction, OrderSize size, int sizeValue,
					double priority) implements Comparable<Order>, MarketAttributes {
	public static final String MARKET_ATTRIBUTE_NAME = "Market";
	public static final String ORDER_ATTRIBUTE_NAME = "Order";
	public static final String SIZE_ATTRIBUTE_NAME = "Size";
	public static final String PRIORITY_ATTRIBUTE_NAME = "Priority";

	@Override
	public int compareTo(Order o) {
		return Double.compare(priority, o.priority);
	}

	@Override
	public Map<String, String> toAttributes() {
		return new HashMap<>(Map.of(MARKET_ATTRIBUTE_NAME, market.name(), ORDER_ATTRIBUTE_NAME, direction().toString(), SIZE_ATTRIBUTE_NAME,
				formatSize(), PRIORITY_ATTRIBUTE_NAME, format("%.2f", priority)));
	}

	public String formatSize() {
		return switch (size) {
			case COUNT -> format("%d shares", sizeValue);
			case PERCENTAGE -> format("%d %%", sizeValue);
		};
	}
}
