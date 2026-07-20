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
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StooqLoaderTests {

	private static final Path TEST_RESOURCES = Path.of("src", "test", "resources").toAbsolutePath();

	private static Market loadPLStooqMarket(MarketName name) throws ExecutionException, InterruptedException {
		MarketData data = StooqLoader.loadPL(TEST_RESOURCES.toString());
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

	@Test
	void shouldLoadPLFromStooqDataDirectory() throws ExecutionException, InterruptedException {
		MarketData data = StooqLoader.loadPL(TEST_RESOURCES.resolve("stooq_data").toString());

		assertNotNull(data.getMarket(new MarketName("ind")));
		assertNotNull(data.getMarket(new MarketName("stk")));
	}

	@Test
	void shouldLoadPLDirectlyFromPLDirectory() throws ExecutionException, InterruptedException {
		Path plDirectory = TEST_RESOURCES.resolve(Path.of("stooq_data", "data", "daily", "pl"));

		MarketData data = StooqLoader.loadPL(plDirectory.toString());

		assertNotNull(data.getMarket(new MarketName("ind")));
		assertNotNull(data.getMarket(new MarketName("stk")));
	}

	@Test
	void shouldRejectMissingDirectory(@TempDir Path temporaryDirectory) {
		Path missingDirectory = temporaryDirectory.resolve("missing");

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> StooqLoader.loadPL(missingDirectory.toString()));

		assertTrue(exception.getMessage().contains("does not exist"));
	}

	@Test
	void shouldRejectDirectoryWithoutStooqData(@TempDir Path temporaryDirectory) throws IOException {
		Files.createDirectory(temporaryDirectory.resolve("unrelated"));

		IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
				() -> StooqLoader.loadPL(temporaryDirectory.toString()));

		assertTrue(exception.getMessage().contains("does not contain unpacked Stooq PL data"));
	}

	@Test
	void shouldLoadEmptyPLDirectory(@TempDir Path temporaryDirectory) throws IOException, ExecutionException, InterruptedException {
		Path plDirectory = Files.createDirectories(temporaryDirectory.resolve(Path.of("stooq_data", "data", "daily", "pl")));

		MarketData data = StooqLoader.loadPL(temporaryDirectory.toString());

		assertTrue(data.listMarkets(market -> true).isEmpty());
		assertFalse(Files.exists(plDirectory.resolve("wse stocks")));
	}

}
