package com.alphatica.alis.tools.data.loader.stooq;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.loader.stooq.StooqLoader;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.tools.data.FloatArraySlice;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StooqLoaderTests {

	private static Market loadPLStooqMarket(MarketName name) throws ExecutionException, InterruptedException {
		String path = Path.of("src", "test", "resources").toAbsolutePath().toString();
		MarketData data = StooqLoader.loadPL(path);
		return data.getMarket(name);
	}

	@Test
	void shouldLoadPLStock() throws ExecutionException, InterruptedException {
		Market market = loadPLStooqMarket(new MarketName("ind"));
		assertEquals(MarketType.INDICE, market.getType());
		Time timeFirst = new Time(20100104);
		TimeMarketData firstTimeMarketData = market.getAtOrNext(timeFirst);
		assertEquals(timeFirst, firstTimeMarketData.getTime());
		double open = firstTimeMarketData.getData(Layer.OPEN, 0);
		assertEquals(1.1, open, 0.001);

		Time middleTime = new Time(20100110);

		TimeMarketData beforeMiddleTimeMarketData = market.getAtOrPrevious(middleTime);
		assertTrue(beforeMiddleTimeMarketData.getTime().isBefore(middleTime));
		assertEquals(5.2, beforeMiddleTimeMarketData.getData(Layer.CLOSE, 0), 0.001);

		TimeMarketData afterMiddleTimeMarketData = market.getAtOrNext(middleTime);
		assertTrue(afterMiddleTimeMarketData.getTime().isAfter(middleTime));
		assertEquals(6.5, afterMiddleTimeMarketData.getData(Layer.HIGH, 0), 0.001);
	}

	@Test
	void shouldLoadPLPartOfHistory() throws ExecutionException, InterruptedException {
		Market market = loadPLStooqMarket(new MarketName("ind"));
		TimeMarketData marketData = market.getAtOrNext(new Time(20100111));
		FloatArraySlice closes = marketData.getLayer(Layer.CLOSE);
		assertEquals(6.2, closes.get(0), 0.001);
		assertEquals(5.2, closes.get(1), 0.001);
		assertEquals(4.2, closes.get(2), 0.001);
		assertEquals(3.2, closes.get(3), 0.001);
		assertEquals(2.2, closes.get(4), 0.001);
		assertEquals(1.2, closes.get(5), 0.001);
	}

}
