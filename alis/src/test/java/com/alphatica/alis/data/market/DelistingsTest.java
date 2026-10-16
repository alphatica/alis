package com.alphatica.alis.data.market;

import com.alphatica.alis.data.time.Time;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DelistingsTest {

	private static final List<Time> SESSIONS = List.of(new Time(10), new Time(20), new Time(30));

	@Test
	void shouldAssignDelistingToSessionWithTheSameDate() {
		Delisting delisting = new Delisting(new MarketName("a"), new Time(20), 1.0);
		Delistings delistings = new Delistings(List.of(delisting), SESSIONS);

		assertEquals(List.of(delisting), delistings.at(new Time(20)));
		assertTrue(delistings.at(new Time(10)).isEmpty());
	}

	@Test
	void shouldAssignDelistingToFirstSessionAfterDate() {
		Delisting delisting = new Delisting(new MarketName("a"), new Time(11), 1.0);
		Delistings delistings = new Delistings(List.of(delisting), SESSIONS);

		assertEquals(List.of(delisting), delistings.at(new Time(20)));
	}

	@Test
	void shouldIgnoreDelistingAfterLastSession() {
		Delisting delisting = new Delisting(new MarketName("a"), new Time(31), 1.0);
		Delistings delistings = new Delistings(List.of(delisting), SESSIONS);

		assertEquals(List.of(delisting), delistings.all());
		SESSIONS.forEach(session -> assertTrue(delistings.at(session).isEmpty()));
	}

	@Test
	void shouldGroupDelistingsBySession() {
		Delisting a = new Delisting(new MarketName("a"), new Time(15), 1.0);
		Delisting b = new Delisting(new MarketName("b"), new Time(20), 0.0);
		Delistings delistings = new Delistings(List.of(a, b), SESSIONS);

		assertEquals(List.of(a, b), delistings.at(new Time(20)));
		assertEquals(Map.of(new Time(20), List.of(a, b)), delistings.bySession());
	}
}
