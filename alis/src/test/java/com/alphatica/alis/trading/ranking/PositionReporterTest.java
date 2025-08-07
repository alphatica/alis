package com.alphatica.alis.trading.ranking;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PositionReporterTest {

	@Test
	void shouldReturnZeroForNonMatchingPosition() {
		var positionReports = new PositionReporter();
		positionReports.report(new PositionReport("s1", new Time(1), new MarketName("m1"), 1.0f));
		// non-matching source
		assertEquals(0, positionReports.getPosition(new Time(1), "s2", new MarketName("m1")));
		// non-matching time
		assertEquals(0, positionReports.getPosition(new Time(2), "s1", new MarketName("m1")));
		// non-matching market
		assertEquals(0, positionReports.getPosition(new Time(1), "s1", new MarketName("m2")));
	}

}