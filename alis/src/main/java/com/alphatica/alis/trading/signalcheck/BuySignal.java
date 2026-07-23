package com.alphatica.alis.trading.signalcheck;

public record BuySignal(double requestedAllocation, double priority) {

	public BuySignal {
		if (!Double.isFinite(requestedAllocation) || requestedAllocation <= 0.0) {
			throw new IllegalArgumentException("requestedAllocation must be finite and positive");
		}
		if (!Double.isFinite(priority)) {
			throw new IllegalArgumentException("priority must be finite");
		}
	}
}
