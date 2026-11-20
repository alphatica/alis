package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.data.StandardMarketData;
import com.alphatica.alis.data.market.Delisting;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.TestMarket;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.alphatica.alis.data.market.MarketFilters.ALL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReducedMarketDataTest {

	private static final MarketName A = new MarketName("a");
	private static final MarketName B = new MarketName("b");
	private static final Time T1 = new Time(20200101);
	private static final Time T2 = new Time(20200102);
	private static final Time T3 = new Time(20200103);
	private static final Time T4 = new Time(20200104);

	private final StandardMarketData base = baseData();
	private final ReducedMarketData reduced = new ReducedMarketData(base, Set.of(B));

	@Test
	void shouldHideExcludedMarkets() {
		assertNotNull(reduced.getMarket(A));
		assertNull(reduced.getMarket(B));
		assertEquals(List.of(A), reduced.listMarkets(ALL).stream().map(Market::getName).toList());
	}

	@Test
	void shouldUseTimesOfRemainingMarkets() {
		assertEquals(List.of(T1, T2, T3), reduced.getTimes());
	}

	@Test
	void shouldAssignAllDelistingsToRemainingSessions() {
		Delisting delistingA = new Delisting(A, T2, 1.0);
		Delisting delistingB = new Delisting(B, T4, 1.0);
		base.setDelistings(List.of(delistingA, delistingB));

		ReducedMarketData withDelistings = new ReducedMarketData(base, Set.of(B));

		assertEquals(List.of(delistingA, delistingB), withDelistings.getDelistings());
		assertEquals(List.of(delistingA), withDelistings.getDelistingsAt(T2));
		assertEquals(Map.of(T2, List.of(delistingA)), withDelistings.getDelistingsBySession());
	}

	@Test
	void shouldReturnSameCachedSnapshot() {
		assertSame(reduced.cachedSnapshotAt(T2), reduced.cachedSnapshotAt(T2));
	}

	@Test
	void shouldShareMarketDataWithBaseCachedSnapshot() {
		TimeMarketDataSet snapshot = reduced.cachedSnapshotAt(T2);

		assertSame(base.cachedSnapshotAt(T2).get(A), snapshot.get(A));
		assertNull(snapshot.get(B));
		assertNotNull(base.cachedSnapshotAt(T2).get(B));
	}

	@Test
	void shouldNotShareCachedValuesWithBaseSnapshot() {
		TimeMarketDataSet snapshot = reduced.cachedSnapshotAt(T2);

		snapshot.cached("key", data -> "reduced");

		assertEquals("base", base.cachedSnapshotAt(T2).cached("key", data -> "base"));
	}

	@Test
	void shouldExcludeMarketsFromUncachedSnapshot() {
		TimeMarketDataSet snapshot = reduced.snapshotAt(T3);

		assertNotSame(reduced.cachedSnapshotAt(T3), snapshot);
		assertNotNull(snapshot.get(A));
		assertNull(snapshot.get(B));
		assertTrue(snapshot.listAllKnownMarkets(m -> true).stream().allMatch(m -> m.getMarketName().equals(A)));
	}

	private static StandardMarketData baseData() {
		StandardMarketData data = new StandardMarketData();
		data.addMarkets(Map.of(
				A, TestMarket.withCloses(A, new int[]{20200101, 20200102, 20200103}, new float[]{1, 2, 3}),
				B, TestMarket.withCloses(B, new int[]{20200102, 20200103, 20200104}, new float[]{4, 5, 6})));
		return data;
	}
}
