package com.alphatica.alis.trading.signalcheck;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;

import java.util.Objects;

public record TradeOpportunity(
		MarketName market,
		Time signalTime,
		Time openTime,
		Time closeTime,
		int openEventIndex,
		int closeEventIndex,
		float effectiveOpenPrice,
		float effectiveClosePrice,
		int bars,
		double requestedAllocation,
		double priority
) {
	public TradeOpportunity {
		Objects.requireNonNull(market, "market");
		Objects.requireNonNull(signalTime, "signalTime");
		Objects.requireNonNull(openTime, "openTime");
		Objects.requireNonNull(closeTime, "closeTime");
		if (openEventIndex < 0 || closeEventIndex <= openEventIndex) {
			throw new IllegalArgumentException("closeEventIndex must be greater than openEventIndex");
		}
		if (!Float.isFinite(effectiveOpenPrice) || effectiveOpenPrice <= 0.0f
				|| !Float.isFinite(effectiveClosePrice) || effectiveClosePrice <= 0.0f) {
			throw new IllegalArgumentException("effective prices must be finite and positive");
		}
		if (bars <= 0) {
			throw new IllegalArgumentException("bars must be positive");
		}
		if (!Double.isFinite(requestedAllocation) || requestedAllocation <= 0.0) {
			throw new IllegalArgumentException("requestedAllocation must be finite and positive");
		}
		if (!Double.isFinite(priority)) {
			throw new IllegalArgumentException("priority must be finite");
		}
	}
}
