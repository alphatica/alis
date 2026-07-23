package com.alphatica.alis.trading.signalcheck;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AllocationReplayerTest {
	private final AllocationReplayer replayer = new AllocationReplayer();

	@Test
	void shouldSortByPriorityThenMarketAndIgnorePositivePriorityScaling() {
		var low = opportunity("a", 0, 2, 1.0, 1.0);
		var marketZ = opportunity("z", 0, 2, 1.0, 2.0);
		var marketB = opportunity("b", 0, 2, 1.0, 2.0);
		var marketA = opportunity("a", 0, 2, 1.0, 2.0);
		var execution = execution(List.of(low, marketZ, marketB, marketA), 3);

		var replay = replayer.replay(execution, 1.0, AllocationPolicy.STOP_ON_FIRST_REJECTION);
		assertEquals(List.of(new MarketName("a")), replay.acceptedTrades().stream()
				.map(selected -> selected.opportunity().market()).toList());
		assertEquals(3, replay.rejectedTrades());

		var scaled = execution(List.of(
				opportunity("a", 0, 2, 1.0, 10.0),
				opportunity("z", 0, 2, 1.0, 20.0),
				opportunity("b", 0, 2, 1.0, 20.0),
				opportunity("a", 0, 2, 1.0, 20.0)), 3);
		assertEquals(List.of(new MarketName("a")), replayer.replay(scaled, 1.0,
				AllocationPolicy.STOP_ON_FIRST_REJECTION).acceptedTrades().stream()
				.map(selected -> selected.opportunity().market()).toList());
	}

	@Test
	void stopPolicyShouldRejectTheFirstOversizedCandidateAndEveryWeakerCandidateAtThatTime() {
		var strong = opportunity("a", 0, 1, 1.5, 10.0);
		var weak = opportunity("b", 0, 1, 0.5, 1.0);
		var replay = replayer.replay(execution(List.of(weak, strong), 2), 1.0,
				AllocationPolicy.STOP_ON_FIRST_REJECTION);

		assertTrue(replay.acceptedTrades().isEmpty());
		assertEquals(2, replay.rejectedTrades());
		assertEquals(0.0, replay.averageUsedAllocation());
	}

	@Test
	void partialPolicyShouldGiveAllRemainingCapacityToTheFirstOversizedCandidate() {
		var strong = opportunity("a", 0, 1, 1.5, 10.0);
		var weak = opportunity("b", 0, 1, 0.5, 1.0);
		var replay = replayer.replay(execution(List.of(weak, strong), 2), 1.0,
				AllocationPolicy.PARTIAL_LAST_POSITION);

		assertEquals(1, replay.acceptedTradeCount());
		assertEquals(strong, replay.acceptedTrades().getFirst().opportunity());
		assertEquals(1.0, replay.acceptedTrades().getFirst().actualAllocation());
		assertEquals(1, replay.rejectedTrades());
	}

	@Test
	void shouldReleaseActualPartialAllocationBeforeOpeningAtTheSameEvent() {
		var first = opportunity("a", 0, 1, 2.0, 10.0);
		var next = opportunity("b", 1, 2, 1.0, 1.0);
		var replay = replayer.replay(execution(List.of(next, first), 3), 1.0,
				AllocationPolicy.PARTIAL_LAST_POSITION);

		assertEquals(2, replay.acceptedTradeCount());
		assertEquals(List.of(1.0, 1.0), replay.acceptedTrades().stream()
				.map(SelectedTrade::actualAllocation).toList());
		assertEquals(2.0 / 3.0, replay.averageUsedAllocation());
		assertEquals(2.0 / 3.0, replay.averageUtilization());
	}

	@Test
	void shouldUseEpsilonAtTheCapacityBoundary() {
		var almostExact = opportunity("a", 0, 1, 1.0 + AllocationReplayer.EPSILON / 2.0, 1.0);
		var replay = replayer.replay(execution(List.of(almostExact), 2), 1.0,
				AllocationPolicy.STOP_ON_FIRST_REJECTION);

		assertEquals(1, replay.acceptedTradeCount());
		assertEquals(almostExact.requestedAllocation(), replay.acceptedTrades().getFirst().actualAllocation());
		assertEquals(0.5, replay.averageUsedAllocation(), 0.000000001);
	}

	@Test
	void shouldAcceptEverythingForUnlimitedAllocationAndRejectInvalidLimits() {
		var execution = execution(List.of(
				opportunity("a", 0, 1, 100.0, 1.0),
				opportunity("b", 0, 1, 200.0, 2.0)), 2);
		var replay = replayer.replay(execution, Double.POSITIVE_INFINITY,
				AllocationPolicy.STOP_ON_FIRST_REJECTION);

		assertEquals(2, replay.acceptedTradeCount());
		assertEquals(150.0, replay.averageUsedAllocation());
		assertTrue(Double.isNaN(replay.averageUtilization()));
		assertThrows(IllegalArgumentException.class,
				() -> replayer.replay(execution, 0.0, AllocationPolicy.STOP_ON_FIRST_REJECTION));
		assertThrows(IllegalArgumentException.class,
				() -> replayer.replay(execution, Double.NaN, AllocationPolicy.STOP_ON_FIRST_REJECTION));
	}

	@Test
	void shouldReplayManyLimitsInArgumentOrderWithoutMutatingTheLedger() {
		var opportunity = opportunity("a", 0, 1, 1.0, 1.0);
		var execution = execution(List.of(opportunity), 2);
		List<AllocationReplayResult> results = replayer.replayAll(execution,
				List.of(2.0, 0.5, 1.0), AllocationPolicy.STOP_ON_FIRST_REJECTION);

		assertEquals(List.of(2.0, 0.5, 1.0), results.stream()
				.map(AllocationReplayResult::maxAllocation).toList());
		assertEquals(List.of(1, 0, 1), results.stream()
				.map(AllocationReplayResult::acceptedTradeCount).toList());
		assertEquals(List.of(opportunity), execution.opportunities());
		assertThrows(UnsupportedOperationException.class, results::clear);
	}

	@Test
	void shouldHandleAnEmptyLedger() {
		var replay = replayer.replay(execution(List.of(), 0), 1.0,
				AllocationPolicy.STOP_ON_FIRST_REJECTION);
		assertTrue(replay.acceptedTrades().isEmpty());
		assertEquals(0.0, replay.averageUsedAllocation());
		assertEquals(0.0, replay.averageUtilization());
	}

	private static SignalExecutionResult execution(List<TradeOpportunity> opportunities, int times) {
		List<Time> executionTimes = java.util.stream.IntStream.range(0, times)
				.mapToObj(Time::new).toList();
		return new SignalExecutionResult(new Time(0), new Time(Math.max(0, times - 1)),
				executionTimes, opportunities);
	}

	private static TradeOpportunity opportunity(String market, int openEvent, int closeEvent,
			double allocation, double priority) {
		return new TradeOpportunity(new MarketName(market), new Time(openEvent), new Time(openEvent),
				new Time(closeEvent), openEvent, closeEvent,
				100.0f, 110.0f, Math.max(1, closeEvent - openEvent), allocation, priority);
	}
}
