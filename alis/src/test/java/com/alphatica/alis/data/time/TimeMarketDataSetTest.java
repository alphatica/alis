package com.alphatica.alis.data.time;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static java.util.concurrent.TimeUnit.SECONDS;

class TimeMarketDataSetTest {

	private final TimeMarketDataSet set = new TimeMarketDataSet(Map.of(), new Time(1));

	@Test
	void shouldComputeOnceForEqualKeys() {
		AtomicInteger calls = new AtomicInteger();

		List<Integer> first = set.cached(new Key("a", 1), data -> List.of(calls.incrementAndGet()));
		List<Integer> second = set.cached(new Key("a", 1), data -> List.of(calls.incrementAndGet()));

		assertEquals(List.of(1), first);
		assertSame(first, second);
		assertEquals(1, calls.get());
	}

	@Test
	void shouldCacheSeparatelyForDifferentKeys() {
		assertEquals("A", set.cached(new Key("a", 1), data -> "A"));
		assertEquals("B", set.cached(new Key("b", 2), data -> "B"));

		assertEquals("A", set.cached(new Key("a", 1), data -> "other"));
	}

	@Test
	void shouldPassItselfToCacheComputation() {
		TimeMarketDataSet received = set.cached(new Key("a", 1), data -> data);

		assertSame(set, received);
	}

	@Test
	void shouldNotCacheNull() {
		assertNull(set.cached(new Key("a", 1), data -> null));

		assertEquals("value", set.cached(new Key("a", 1), data -> "value"));
	}

	@Test
	void shouldNotCacheWhenComputationThrows() {
		IllegalArgumentException thrown = new IllegalArgumentException("failed");

		IllegalArgumentException actual = assertThrows(IllegalArgumentException.class,
				() -> set.cached(new Key("a", 1), data -> {
					throw thrown;
				}));

		assertSame(thrown, actual);
		assertEquals("value", set.cached(new Key("a", 1), data -> "value"));
	}

	@Test
	void shouldRejectRecursiveComputationForSameKey() {
		Key key = new Key("a", 1);

		assertThrows(IllegalStateException.class, () -> set.cached(key, data -> data.cached(key, ignored -> "inner")));

		assertEquals("value", set.cached(key, data -> "value"));
	}

	@Test
	void shouldMakeOtherThreadWaitForValueBeingComputed() throws InterruptedException {
		Key key = new Key("a", 1);
		AtomicInteger calls = new AtomicInteger();
		CountDownLatch computing = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		AtomicReference<Object> firstResult = new AtomicReference<>();
		AtomicReference<Object> secondResult = new AtomicReference<>();
		Thread first = Thread.ofPlatform().daemon().start(() -> firstResult.set(set.cached(key, data -> {
			computing.countDown();
			awaitQuietly(release);
			return List.of(calls.incrementAndGet());
		})));
		assertTrue(computing.await(5, SECONDS));
		Thread second = Thread.ofPlatform().daemon().start(() -> secondResult.set(
				set.cached(key, data -> List.of(calls.incrementAndGet()))));

		try {
			waitUntilBlockedOrFinished(second);
		} finally {
			release.countDown();
		}
		first.join(5_000);
		second.join(5_000);

		assertEquals(List.of(1), firstResult.get());
		assertSame(firstResult.get(), secondResult.get());
		assertEquals(1, calls.get());
	}

	private static void waitUntilBlockedOrFinished(Thread thread) {
		long deadline = System.nanoTime() + 5_000_000_000L;
		while (thread.getState() != Thread.State.BLOCKED && thread.getState() != Thread.State.WAITING
				&& thread.getState() != Thread.State.TERMINATED) {
			if (System.nanoTime() > deadline) {
				fail("Second thread did not reach cache");
			}
			Thread.onSpinWait();
		}
	}

	private static void awaitQuietly(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private record Key(String name, int hash) {
		@Override
		public boolean equals(Object o) {
			return o instanceof Key other && other.name.equals(name);
		}

		@Override
		public int hashCode() {
			return hash;
		}
	}
}
