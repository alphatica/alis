package com.alphatica.alis.trading.signalcheck;

import com.alphatica.alis.trading.signalcheck.scoregenerator.ScoreCalculator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AllocationScorer {
	private final AllocationReplayer replayer;

	public AllocationScorer() {
		this(new AllocationReplayer());
	}

	public AllocationScorer(AllocationReplayer replayer) {
		this.replayer = Objects.requireNonNull(replayer, "replayer");
	}

	public List<AllocationScore> calculateScores(SignalExecutionResult execution,
			List<Double> maxAllocations, AllocationPolicy policy, ScoreCalculator calculator) {
		Objects.requireNonNull(calculator, "calculator");
		List<AllocationReplayResult> replays = replayer.replayAll(execution, maxAllocations, policy);
		List<AllocationScore> scores = new ArrayList<>(replays.size());
		for (AllocationReplayResult replay : replays) {
			scores.add(new AllocationScore(replay, calculator.calculate(execution, replay)));
		}
		return scores;
	}
}
