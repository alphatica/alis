package com.alphatica.alis.trading.signalcheck.scoregenerator;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.signalcheck.AllocationPolicy;
import com.alphatica.alis.trading.signalcheck.AllocationReplayResult;
import com.alphatica.alis.trading.signalcheck.SelectedTrade;
import com.alphatica.alis.trading.signalcheck.SignalExecutionResult;
import com.alphatica.alis.trading.signalcheck.TradeOpportunity;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreCalculatorTest {
	@Test
	void shouldWeightProfitAndBarsByActualAllocationWithoutUsingPriority() {
		TradeOpportunity first = opportunity("m1", 100.0f, 200.0f, 1, 1.0, 1.0);
		TradeOpportunity second = opportunity("m2", 20.0f, 30.0f, 2, 2.0, 1_000.0);
		TradeOpportunity third = opportunity("m3", 30.0f, 15.0f, 1, 1.0, -50.0);
		SignalExecutionResult execution = execution(List.of(first, second, third));
		AllocationReplayResult replay = replay(4.0, List.of(
				new SelectedTrade(first, 1.0),
				new SelectedTrade(second, 2.0),
				new SelectedTrade(third, 0.5)));

		double score = new ArithmeticAverageProfitPerBarScoreCalculator().calculate(execution, replay);
		assertEquals(175.0 / 5.5, score, 0.000001);
	}

	@Test
	void shouldPenalizeUnusedFiniteCapacity() {
		TradeOpportunity opportunity = opportunity("m", 100.0f, 200.0f, 1, 1.0, 1.0);
		SignalExecutionResult execution = execution(List.of(opportunity));
		AllocationReplayResult replay = replay(4.0, List.of(new SelectedTrade(opportunity, 1.0)));

		assertEquals(100.0 / 12.0,
				new CapacityAdjustedScoreCalculator().calculate(execution, replay), 0.000001);
	}

	@Test
	void shouldDefineEmptyAndUnlimitedEdgeCasesExplicitly() {
		SignalExecutionResult empty = execution(List.of());
		SignalExecutionResult emptyTimeline = new SignalExecutionResult(new Time(1), new Time(1),
				List.of(), List.of());
		AllocationReplayResult finite = replay(1.0, List.of());
		AllocationReplayResult unlimited = new AllocationReplayResult(Double.POSITIVE_INFINITY,
				AllocationPolicy.STOP_ON_FIRST_REJECTION, List.of(), 0, 0.0, Double.NaN);

		assertTrue(Double.isNaN(new ArithmeticAverageProfitPerBarScoreCalculator().calculate(empty, finite)));
		assertEquals(0.0, new CapacityAdjustedScoreCalculator().calculate(empty, finite));
		assertTrue(Double.isNaN(new CapacityAdjustedScoreCalculator().calculate(emptyTimeline, finite)));
		assertThrows(IllegalArgumentException.class,
				() -> new CapacityAdjustedScoreCalculator().calculate(empty, unlimited));
	}

	private static SignalExecutionResult execution(List<TradeOpportunity> opportunities) {
		return new SignalExecutionResult(new Time(1), new Time(3),
				List.of(new Time(1), new Time(2), new Time(3)), opportunities);
	}

	private static AllocationReplayResult replay(double maxAllocation, List<SelectedTrade> trades) {
		return new AllocationReplayResult(maxAllocation, AllocationPolicy.PARTIAL_LAST_POSITION,
				trades, 0, 1.0, 0.25);
	}

	private static TradeOpportunity opportunity(String market, float open, float close, int bars,
			double requestedAllocation, double priority) {
		return new TradeOpportunity(new MarketName(market), new Time(1), new Time(1), new Time(2),
				0, 2, open, close, bars, requestedAllocation, priority);
	}
}
