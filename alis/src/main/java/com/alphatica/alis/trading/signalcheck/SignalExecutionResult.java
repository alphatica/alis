package com.alphatica.alis.trading.signalcheck;

import com.alphatica.alis.data.time.Time;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public record SignalExecutionResult(
		Time startTime,
		Time endTime,
		List<Time> executionTimes,
		List<TradeOpportunity> opportunities
) {
	public SignalExecutionResult {
		Objects.requireNonNull(startTime, "startTime");
		Objects.requireNonNull(endTime, "endTime");
		if (endTime.isBefore(startTime)) {
			throw new IllegalArgumentException("endTime must not be before startTime");
		}
		executionTimes = List.copyOf(executionTimes);
		opportunities = List.copyOf(opportunities);
		validateTimes(executionTimes);
		validateOpportunities(opportunities, executionTimes.size());
	}

	private static void validateTimes(List<Time> times) {
		Time previous = null;
		for (Time time : times) {
			if (previous != null && time.compareTo(previous) <= 0) {
				throw new IllegalArgumentException("executionTimes must be strictly increasing");
			}
			previous = time;
		}
	}

	private static void validateOpportunities(List<TradeOpportunity> opportunities, int eventCount) {
		Set<TradeOpportunity> uniqueOpportunities = new HashSet<>();
		for (TradeOpportunity opportunity : opportunities) {
			if (!uniqueOpportunities.add(opportunity)) {
				throw new IllegalArgumentException("opportunities must be unique");
			}
			if (opportunity.openEventIndex() >= eventCount
					|| opportunity.closeEventIndex() > eventCount) {
				throw new IllegalArgumentException("opportunity event index is outside executionTimes");
			}
		}
	}
}
