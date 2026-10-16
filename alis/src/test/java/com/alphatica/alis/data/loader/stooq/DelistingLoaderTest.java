package com.alphatica.alis.data.loader.stooq;

import com.alphatica.alis.data.loader.DataProcessingException;
import com.alphatica.alis.data.market.Delisting;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelistingLoaderTest {

	@Test
	void shouldParseDelistings() {
		List<Delisting> delistings = DelistingLoader.parse(List.of("date,ticker,price", "20190913,hld,0.986", "20260505,vtl,0.0"));

		assertEquals(List.of(
				new Delisting(new MarketName("hld"), new Time(20190913), 0.986),
				new Delisting(new MarketName("vtl"), new Time(20260505), 0.0)), delistings);
	}

	@Test
	void shouldParseOnlyHeader() {
		assertTrue(DelistingLoader.parse(List.of("date,ticker,price")).isEmpty());
	}

	@Test
	void shouldReturnEmptyListWhenFileIsMissing(@TempDir Path directory) {
		assertTrue(DelistingLoader.load(directory.resolve("delisted.csv")).isEmpty());
	}

	@Test
	void shouldLoadFile(@TempDir Path directory) throws IOException {
		Path file = Files.writeString(directory.resolve("delisted.csv"), "date,ticker,price\n20190913,hld,0.986\n");

		assertEquals(List.of(new Delisting(new MarketName("hld"), new Time(20190913), 0.986)), DelistingLoader.load(file));
	}

	@Test
	void shouldRejectInvalidLines() {
		assertInvalid("20190913,hld", "Invalid delisted line 2: expected 3 fields, got 2");
		assertInvalid("abc,hld,1.0", "Invalid delisted line 2: invalid date 'abc'");
		assertInvalid("20190913,hld,x", "Invalid delisted line 2: invalid price 'x'");
		assertInvalid("20190913,hld,-1.0", "Invalid delisted line 2: Value -1.000000 at index 0 is not >= 0.0");
		assertInvalid("20190913,hld,NaN", "Invalid delisted line 2: Value NaN at index 0 is not >= 0.0");
		assertInvalid("20190913,,1.0", "Invalid delisted line 2: empty ticker");
	}

	private static void assertInvalid(String line, String expectedMessage) {
		List<String> lines = List.of("date,ticker,price", line);
		DataProcessingException exception = assertThrows(DataProcessingException.class, () -> DelistingLoader.parse(lines));
		assertEquals(expectedMessage, exception.getException().getMessage());
	}
}
