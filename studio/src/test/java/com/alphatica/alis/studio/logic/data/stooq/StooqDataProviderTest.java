package com.alphatica.alis.studio.logic.data.stooq;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.studio.state.AppState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class StooqDataProviderTest {

	@Test
	void shouldLoadPLDataFromProvidedDirectory(@TempDir Path temporaryDirectory) throws IOException {
		Path stocksDirectory = Files.createDirectories(
				temporaryDirectory.resolve(Path.of("stooq_data", "data", "daily", "pl", "wse stocks")));
		Files.writeString(stocksDirectory.resolve("abc.txt"), """
				<TICKER>,<PER>,<DATE>,<TIME>,<OPEN>,<HIGH>,<LOW>,<CLOSE>,<VOL>,<OPENINT>
				ABC,D,20260717,000000,10.0,12.0,9.0,11.0,100,0
				""");

		StooqDataProvider.loadPLData(temporaryDirectory);

		assertEquals("Data loaded", AppState.getDataStatus());
		assertNotNull(AppState.getMarketData());
		assertNotNull(AppState.getMarketData().getMarket(new MarketName("abc")));
	}
}
