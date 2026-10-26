package com.alphatica.alis.studio.logic.options;

import com.alphatica.alis.data.StandardMarketData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StudioOptionsTest {

	@Test
	void shouldRefuseJensensAlphaWithoutBenchmarkInMarketData() {
		ScorerOption alpha = StudioOptions.SCORERS.get("Jensen's alpha");

		UnavailableScorerException exception = assertThrows(UnavailableScorerException.class,
				() -> alpha.forData(new StandardMarketData()));

		assertEquals("Jensen's alpha needs the benchmark wig in the loaded market data", exception.getMessage());
	}

	@Test
	void shouldOfferScorersNotNeedingMarketDataWithoutIt() throws UnavailableScorerException {
		assertNotNull(StudioOptions.SCORERS.get("Net asset value").forData(new StandardMarketData()).get());
	}
}
