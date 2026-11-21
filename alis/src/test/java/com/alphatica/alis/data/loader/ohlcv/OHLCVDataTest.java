package com.alphatica.alis.data.loader.ohlcv;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.layer.Layer.HIGH;
import static com.alphatica.alis.data.layer.Layer.LOW;
import static com.alphatica.alis.data.layer.Layer.MV;
import static com.alphatica.alis.data.layer.Layer.OPEN;
import static com.alphatica.alis.data.layer.Layer.PE;
import static com.alphatica.alis.data.layer.Layer.TURNOVER;
import static com.alphatica.alis.data.layer.Layer.VOLUME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OHLCVDataTest {

	private static final String HEADER = "<TICKER>,<PER>,<DATE>,<TIME>,<OPEN>,<HIGH>,<LOW>,<CLOSE>,<VOL>,<OPENINT>";

	@TempDir
	Path directory;

	@Test
	void shouldStoreRowsNewestFirst() throws IOException {
		File file = write("abc.txt", HEADER,
				"ABC,D,20200102,000000,1,4,0.5,2.5,3,0",
				"ABC,D,20200103,000000,2,5,1.5,3.5,10,0");
		OHLCVData ohlcv = OHLCVData.load(file, 2, 4, 5, 6, 7, 8, new Time(0));
		Path pe = write("abc_pe.txt", "header", "ABC,D,20200102,000000,0,0,0,12.5,0,0").toPath();
		ohlcv.updateData(pe.toString(), 2, 7, (row, text) -> row.parseAndSet(text, PE));

		Market market = ohlcv.toMarket(MarketType.STOCK);

		assertEquals(List.of(new Time(20200102), new Time(20200103)), market.getTimes());
		TimeMarketData last = market.getAt(new Time(20200103));
		assertEquals(2, last.getData(OPEN, 0));
		assertEquals(5, last.getData(HIGH, 0));
		assertEquals(1.5f, last.getData(LOW, 0));
		assertEquals(3.5f, last.getData(CLOSE, 0));
		assertEquals(10, last.getData(VOLUME, 0));
		assertEquals(30, last.getData(TURNOVER, 0));
		assertEquals(2.5f, last.getData(CLOSE, 1));
		assertEquals(6, last.getData(TURNOVER, 1));
		assertEquals(12.5f, last.getData(PE, 1));
		assertTrue(Float.isNaN(last.getData(PE, 0)));
		assertTrue(Float.isNaN(last.getData(MV, 1)));
	}

	@Test
	void shouldRejectRepeatedDate() throws IOException {
		File file = write("abc.txt", HEADER,
				"ABC,D,20200102,000000,1,1,1,1,1,0",
				"ABC,D,20200102,000000,2,2,2,2,2,0");

		assertThrows(IllegalArgumentException.class, () -> OHLCVData.load(file, 2, 4, 5, 6, 7, 8, new Time(0)));
	}

	private File write(String name, String... lines) throws IOException {
		Path path = directory.resolve(name);
		Files.write(path, List.of(lines));
		return path.toFile();
	}
}
