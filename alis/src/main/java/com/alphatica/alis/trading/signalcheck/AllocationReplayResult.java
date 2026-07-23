package com.alphatica.alis.trading.signalcheck;

import java.util.List;

import static java.util.Objects.requireNonNull;

public record AllocationReplayResult(
		double maxAllocation,
		AllocationPolicy policy,
		List<SelectedTrade> acceptedTrades,
		int rejectedTrades,
		double averageUsedAllocation,
		double averageUtilization
) {
	public AllocationReplayResult {
		AllocationReplayer.validateMaxAllocation(maxAllocation);
		requireNonNull(policy, "policy");
		acceptedTrades = List.copyOf(acceptedTrades);
		if (rejectedTrades < 0) {
			throw new IllegalArgumentException("rejectedTrades must not be negative");
		}
		if (!Double.isFinite(averageUsedAllocation) || averageUsedAllocation < 0.0) {
			throw new IllegalArgumentException("averageUsedAllocation must be finite and non-negative");
		}
		if (Double.isFinite(maxAllocation)) {
			if (!Double.isFinite(averageUtilization) || averageUtilization < 0.0) {
				throw new IllegalArgumentException("averageUtilization must be finite and non-negative");
			}
		} else if (!Double.isNaN(averageUtilization)) {
			throw new IllegalArgumentException("averageUtilization must be NaN for an unlimited replay");
		}
	}

	public int acceptedTradeCount() {
		return acceptedTrades.size();
	}
}
