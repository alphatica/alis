package com.alphatica.alis.trading.order;

import com.alphatica.alis.data.market.MarketName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderTest {

	private static final MarketName MARKET = new MarketName("m");

	@Test
	void shouldFormatSizeByKind() {
		assertEquals("2.5 shares", new Order(MARKET, Direction.BUY, OrderSize.COUNT, 2.5, 1).formatSize());
		assertEquals("12 %", new Order(MARKET, Direction.BUY, OrderSize.PERCENTAGE, 12, 1).formatSize());
		assertEquals("100.50 value", new Order(MARKET, Direction.BUY, OrderSize.VALUE, 100.5, 1).formatSize());
	}
}
