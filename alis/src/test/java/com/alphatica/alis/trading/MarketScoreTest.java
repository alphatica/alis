package com.alphatica.alis.trading;

import com.alphatica.alis.data.market.MarketName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarketScoreTest {

	@Test
	void shouldShowBetterMarket() {
		var ms1 = new MarketScore(new MarketName("a"), 4.0);
		var ms2 = new MarketScore(new MarketName("a"), 5.0);
		assertEquals(-1, ms1.compareTo(ms2));
		assertEquals(1, ms2.compareTo(ms1));
		assertEquals(0, ms2.compareTo(ms2));
	}

//	@Test
//	void shouldFormatScore() {
//		var ms = new MarketScore(new MarketName("a"), 4.0);
//		assertEquals("a: 4.00", ms.toString());
//	}
}