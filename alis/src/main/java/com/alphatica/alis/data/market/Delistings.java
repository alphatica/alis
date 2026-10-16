package com.alphatica.alis.data.market;

import com.alphatica.alis.data.time.Time;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Assigns each delisting to the first session at or after its date. Delistings after the last session are ignored.
 */
public final class Delistings {
	private final List<Delisting> all;
	private final NavigableMap<Time, List<Delisting>> bySession = new TreeMap<>();

	public Delistings(List<Delisting> delistings, List<Time> sessions) {
		this.all = List.copyOf(delistings);
		for (Delisting delisting : all) {
			int index = Collections.binarySearch(sessions, delisting.time());
			if (index < 0) {
				index = -index - 1;
			}
			if (index < sessions.size()) {
				bySession.computeIfAbsent(sessions.get(index), ignored -> new ArrayList<>()).add(delisting);
			}
		}
	}

	public static Delistings empty() {
		return new Delistings(List.of(), List.of());
	}

	public List<Delisting> all() {
		return all;
	}

	public List<Delisting> at(Time session) {
		return bySession.getOrDefault(session, List.of());
	}

	public NavigableMap<Time, List<Delisting>> bySession() {
		return Collections.unmodifiableNavigableMap(bySession);
	}
}
