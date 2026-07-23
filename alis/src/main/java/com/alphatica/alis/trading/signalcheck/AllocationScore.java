package com.alphatica.alis.trading.signalcheck;

import java.util.Objects;

public record AllocationScore(
		AllocationReplayResult replayResult,
		double score
) {
	public AllocationScore {
		Objects.requireNonNull(replayResult, "replayResult");
	}
}
