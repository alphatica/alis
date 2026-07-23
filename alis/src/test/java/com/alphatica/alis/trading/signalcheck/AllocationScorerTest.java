package com.alphatica.alis.trading.signalcheck;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AllocationScorerTest {
	@Test
	void shouldCalculateAStableCurveFromOneLedger() {
		var trade = new TradeOpportunity(new MarketName("m"), new Time(0), new Time(0), new Time(1),
				0, 1, 100.0f, 110.0f, 1, 1.0, 1.0);
		var execution = new SignalExecutionResult(new Time(0), new Time(1),
				List.of(new Time(0), new Time(1)), List.of(trade));

		List<AllocationScore> scores = new AllocationScorer().calculateScores(execution,
				List.of(1.0, 0.5), AllocationPolicy.PARTIAL_LAST_POSITION,
				(ignoredExecution, replay) -> replay.acceptedTrades().stream()
						.mapToDouble(SelectedTrade::actualAllocation).sum());

		assertEquals(List.of(1.0, 0.5), scores.stream()
				.map(score -> score.replayResult().maxAllocation()).toList());
		assertEquals(List.of(1.0, 0.5), scores.stream().map(AllocationScore::score).toList());
		assertEquals(List.of(1, 1), scores.stream()
				.map(score -> score.replayResult().acceptedTradeCount()).toList());
		assertEquals(List.of(AllocationPolicy.PARTIAL_LAST_POSITION, AllocationPolicy.PARTIAL_LAST_POSITION),
				scores.stream().map(score -> score.replayResult().policy()).toList());
		assertEquals(trade, scores.getFirst().replayResult().acceptedTrades().getFirst().opportunity());
	}
}
