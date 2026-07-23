package com.alphatica.alis.trading.signalcheck;

import static java.util.Objects.requireNonNull;

public record SelectedTrade(TradeOpportunity opportunity, double actualAllocation) {
	public SelectedTrade {
		requireNonNull(opportunity, "opportunity");
		if (!Double.isFinite(actualAllocation) || actualAllocation <= 0.0) {
			throw new IllegalArgumentException("actualAllocation must be finite and positive");
		}
		if (actualAllocation > opportunity.requestedAllocation() + AllocationReplayer.EPSILON) {
			throw new IllegalArgumentException("actualAllocation must not exceed requestedAllocation");
		}
	}
}
