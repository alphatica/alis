package com.alphatica.alis.data.loader.stooq;

import com.alphatica.alis.data.layer.Layer;
import com.alphatica.alis.data.market.MarketCursor;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.alphatica.alis.data.layer.Layer.CLOSE;
import static com.alphatica.alis.data.layer.Layer.OPEN;
import static com.alphatica.alis.data.layer.Layer.VOLUME;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StooqMarketTest {

	private static final MarketName NAME = new MarketName("abc");
	private final StooqMarket market = market(20200102, 20200103, 20200106, 20200107);

	@Test
	void shouldFindSessionAtOrBeforeTime() {
		assertNull(market.getAtOrPrevious(new Time(20200101)));
		assertEquals(new Time(20200102), market.getAtOrPrevious(new Time(20200102)).getTime());
		assertEquals(new Time(20200103), market.getAtOrPrevious(new Time(20200105)).getTime());
		assertEquals(new Time(20200107), market.getAtOrPrevious(new Time(20200110)).getTime());
	}

	@Test
	void shouldFindSessionAtOrAfterTime() {
		assertEquals(new Time(20200102), market.getAtOrNext(new Time(20200101)).getTime());
		assertEquals(new Time(20200103), market.getAtOrNext(new Time(20200103)).getTime());
		assertEquals(new Time(20200106), market.getAtOrNext(new Time(20200104)).getTime());
		assertNull(market.getAtOrNext(new Time(20200108)));
	}

	@Test
	void shouldFindOnlyExactSession() {
		assertEquals(302, market.getAt(new Time(20200103)).getData(CLOSE, 0));
		assertNull(market.getAt(new Time(20200104)));
	}

	@Test
	void shouldGiveSessionValuesAndHistoryOfEveryLayer() {
		TimeMarketData data = market.getAt(new Time(20200106));

		for (Layer layer : Layer.values()) {
			assertEquals(layer.getIndex() * 100 + 3, data.getData(layer, 0));
			assertEquals(layer.getIndex() * 100 + 1, data.getData(layer, 2));
		}
		assertEquals(3, data.getLayer(CLOSE).size());
		assertEquals(302, data.getLayer(CLOSE).get(1));
		assertEquals(401, data.getLayer(VOLUME).get(2));
		assertEquals(152, data.getAveragePrice(1));
	}

	@Test
	void shouldRejectIndexOutsideHistory() {
		TimeMarketData data = market.getAt(new Time(20200103));

		assertEquals(1, data.getData(OPEN, 1));
		assertThrows(IndexOutOfBoundsException.class, () -> data.getData(OPEN, 2));
		assertThrows(IndexOutOfBoundsException.class, () -> data.getData(OPEN, -1));
	}

	@Test
	void shouldListTimes() {
		assertEquals(List.of(new Time(20200102), new Time(20200103), new Time(20200106), new Time(20200107)), market.getTimes());
	}

	@Test
	void shouldRejectLayerWithoutValueForEveryTime() {
		float[][] columns = new float[Layer.values().length][2];
		columns[CLOSE.getIndex()] = new float[1];

		assertThrows(IllegalArgumentException.class, () -> new StooqMarket(NAME, MarketType.STOCK, new int[]{1, 2}, columns));
	}

	@Test
	void shouldMoveCursorSessionBySession() {
		MarketCursor cursor = market.cursor();

		assertNull(cursor.atOrPrevious(new Time(20200101)));
		assertEquals(new Time(20200102), cursor.atOrPrevious(new Time(20200102)).getTime());
		TimeMarketData third = cursor.atOrPrevious(new Time(20200103));
		assertEquals(302, third.getData(CLOSE, 0));
		assertSame(third, cursor.atOrPrevious(new Time(20200103)));
		assertSame(third, cursor.atOrPrevious(new Time(20200105)));
		assertEquals(new Time(20200106), cursor.atOrPrevious(new Time(20200106)).getTime());
		assertEquals(new Time(20200107), cursor.atOrPrevious(new Time(20200107)).getTime());
		assertEquals(new Time(20200107), cursor.atOrPrevious(new Time(20200110)).getTime());
	}

	@Test
	void shouldMoveCursorOverSkippedSessions() {
		MarketCursor cursor = market.cursor();

		assertEquals(new Time(20200103), cursor.atOrPrevious(new Time(20200105)).getTime());
		assertEquals(new Time(20200107), cursor.atOrPrevious(new Time(20200108)).getTime());
		assertEquals(304, cursor.atOrPrevious(new Time(20200108)).getData(CLOSE, 0));
	}

	@Test
	void shouldMoveCursorBack() {
		MarketCursor cursor = market.cursor();
		cursor.atOrPrevious(new Time(20200107));

		assertEquals(new Time(20200103), cursor.atOrPrevious(new Time(20200104)).getTime());
		assertNull(cursor.atOrPrevious(new Time(20200101)));
		assertEquals(new Time(20200102), cursor.atOrPrevious(new Time(20200102)).getTime());
	}

	@Test
	void shouldGiveNothingForMarketWithoutSessions() {
		StooqMarket empty = market();

		assertTrue(empty.getTimes().isEmpty());
		assertNull(empty.getAtOrPrevious(new Time(20200102)));
		assertNull(empty.getAtOrNext(new Time(20200102)));
		assertNull(empty.cursor().atOrPrevious(new Time(20200102)));
	}

	private static StooqMarket market(int... times) {
		float[][] columns = new float[Layer.values().length][times.length];
		for (Layer layer : Layer.values()) {
			for (int i = 0; i < times.length; i++) {
				columns[layer.getIndex()][times.length - 1 - i] = layer.getIndex() * 100 + i + 1;
			}
		}
		return new StooqMarket(NAME, MarketType.STOCK, times, columns);
	}
}
