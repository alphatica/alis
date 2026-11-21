package com.alphatica.alis.data;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.loader.stooq.StooqMarket;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.market.SnapshotCursor;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StandardMarketDataTest {

	private static final MarketName A = new MarketName("a");
	private static final MarketName B = new MarketName("b");
	private static final MarketName C = new MarketName("c");

	private final StandardMarketData data = new StandardMarketData();

	StandardMarketDataTest() {
		data.addMarkets(Map.of(
				C, market(C, new int[]{20200106}, new float[]{30}),
				A, market(A, new int[]{20200102, 20200103, 20200106}, new float[]{1, 2, 3}),
				B, market(B, new int[]{20200103, 20200105}, new float[]{10, 20})));
	}

	@Test
	void shouldUseSessionsOfAllMarkets() {
		assertEquals(List.of(new Time(20200102), new Time(20200103), new Time(20200105), new Time(20200106)), data.getTimes());
	}

	@Test
	void shouldGiveLastKnownSessionOfEveryMarketInNameOrder() {
		TimeMarketDataSet snapshot = data.snapshotAt(new Time(20200105));

		assertEquals(List.of("a@20200103=2.0", "b@20200105=20.0"), describe(snapshot.listAllKnownMarkets(m -> true)));
		assertEquals(List.of("b@20200105=20.0"), describe(snapshot.listUpToDateMarkets(m -> true)));
		assertEquals(2, snapshot.get(A).getData(CLOSE, 0));
		assertNull(snapshot.get(C));
		assertNull(snapshot.get(new MarketName("x")));
	}

	@Test
	void shouldGiveSameSnapshotsWithCursor() {
		SnapshotCursor cursor = data.snapshotCursor();
		int[] times = {20200101, 20200102, 20200103, 20200104, 20200105, 20200106, 20200110, 20200103, 20200106};

		for (int time : times) {
			TimeMarketDataSet expected = data.snapshotAt(new Time(time));
			TimeMarketDataSet actual = cursor.at(new Time(time));
			assertEquals(new Time(time), actual.getTime());
			assertEquals(describe(expected.listAllKnownMarkets(m -> true)), describe(actual.listAllKnownMarkets(m -> true)));
			assertEquals(describe(expected.listUpToDateMarkets(m -> true)), describe(actual.listUpToDateMarkets(m -> true)));
		}
	}

	@Test
	void shouldIncludeMarketsAddedLater() {
		MarketName d = new MarketName("aa");
		data.addMarkets(Map.of(d, market(d, new int[]{20200104}, new float[]{7})));

		TimeMarketDataSet snapshot = data.snapshotCursor().at(new Time(20200104));

		assertEquals(List.of("a@20200103=2.0", "aa@20200104=7.0", "b@20200103=10.0"),
				describe(snapshot.listAllKnownMarkets(m -> true)));
		assertEquals(7, snapshot.get(d).getData(CLOSE, 0));
		assertEquals(10, snapshot.get(B).getData(CLOSE, 0));
	}

	private static List<String> describe(List<TimeMarketData> markets) {
		return markets.stream().map(m -> m.getMarketName() + "@" + m.getTime() + "=" + m.getData(CLOSE, 0)).toList();
	}

	private static StooqMarket market(MarketName name, int[] times, float[] closes) {
		float[][] columns = new float[Layer.values().length][times.length];
		for (float[] column : columns) {
			for (int i = 0; i < closes.length; i++) {
				column[closes.length - 1 - i] = closes[i];
			}
		}
		return new StooqMarket(name, MarketType.STOCK, times, columns);
	}
}
