package com.alphatica.alis.trading.signalcheck;

import static java.util.Objects.requireNonNull;

public record AllocationScore(
		AllocationReplayResult replayResult,
		double score
) {
	public AllocationScore {
		requireNonNull(replayResult, "replayResult");
	}
}
