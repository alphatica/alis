package com.alphatica.alis.trading.signalcheck;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AllocationReplayer {
	public static final double EPSILON = 1.0e-9;

	private static final Comparator<TradeOpportunity> OPEN_ORDER =
			Comparator.comparingDouble(TradeOpportunity::priority).reversed()
					.thenComparing(TradeOpportunity::market);

	public AllocationReplayResult replay(SignalExecutionResult execution, double maxAllocation,
			AllocationPolicy policy) {
		return replayPrepared(prepare(execution), maxAllocation, policy);
	}

	public List<AllocationReplayResult> replayAll(SignalExecutionResult execution,
			List<Double> maxAllocations, AllocationPolicy policy) {
		PreparedEvents events = prepare(execution);
		Objects.requireNonNull(maxAllocations, "maxAllocations");
		List<Double> limits = List.copyOf(maxAllocations);
		List<AllocationReplayResult> results = new ArrayList<>(limits.size());
		for (double limit : limits) {
			results.add(replayPrepared(events, limit, policy));
		}
		return List.copyOf(results);
	}

	public static void validateMaxAllocation(double maxAllocation) {
		if (Double.isNaN(maxAllocation) || maxAllocation <= 0.0) {
			throw new IllegalArgumentException("maxAllocation must be positive or POSITIVE_INFINITY");
		}
	}

	private PreparedEvents prepare(SignalExecutionResult execution) {
		Objects.requireNonNull(execution, "execution");
		int eventCount = execution.executionTimes().size();
		List<List<TradeOpportunity>> opens = createEventLists(eventCount);
		List<List<TradeOpportunity>> closes = createEventLists(eventCount + 1);
		for (TradeOpportunity opportunity : execution.opportunities()) {
			opens.get(opportunity.openEventIndex()).add(opportunity);
			closes.get(opportunity.closeEventIndex()).add(opportunity);
		}
		for (List<TradeOpportunity> candidates : opens) {
			candidates.sort(OPEN_ORDER);
		}
		return new PreparedEvents(opens, closes, execution.opportunities().size());
	}

	private static List<List<TradeOpportunity>> createEventLists(int size) {
		List<List<TradeOpportunity>> result = new ArrayList<>(size);
		for (int index = 0; index < size; index++) {
			result.add(new ArrayList<>());
		}
		return result;
	}

	private AllocationReplayResult replayPrepared(PreparedEvents events, double maxAllocation,
			AllocationPolicy policy) {
		validateMaxAllocation(maxAllocation);
		Objects.requireNonNull(policy, "policy");
		Map<TradeOpportunity, Double> activeAllocations = new HashMap<>();
		List<SelectedTrade> selectedTrades = new ArrayList<>();
		double usedAllocation = 0.0;
		double usedAllocationSum = 0.0;
		int rejectedTrades = 0;

		for (int eventIndex = 0; eventIndex < events.opens.size(); eventIndex++) {
			usedAllocation = closeAccepted(events.closes.get(eventIndex), activeAllocations, usedAllocation);
			Selection selection = selectCandidates(events.opens.get(eventIndex), activeAllocations,
					selectedTrades, usedAllocation, maxAllocation, policy);
			usedAllocation = selection.usedAllocation;
			rejectedTrades += selection.rejectedTrades;
			usedAllocationSum += normalizeSample(usedAllocation, maxAllocation);
		}
		usedAllocation = closeAccepted(events.closes.get(events.opens.size()), activeAllocations, usedAllocation);
		if (!activeAllocations.isEmpty() || Math.abs(usedAllocation) > EPSILON) {
			throw new IllegalStateException("accepted opportunities must be closed by the final phase");
		}

		double averageUsed = events.opens.isEmpty() ? 0.0 : usedAllocationSum / events.opens.size();
		double averageUtilization = Double.isInfinite(maxAllocation)
				? Double.NaN : averageUsed / maxAllocation;
		if (selectedTrades.size() + rejectedTrades != events.opportunityCount) {
			throw new IllegalStateException("every opportunity must be either accepted or rejected");
		}
		return new AllocationReplayResult(maxAllocation, policy, selectedTrades, rejectedTrades,
				averageUsed, averageUtilization);
	}

	private static double closeAccepted(List<TradeOpportunity> closes, Map<TradeOpportunity, Double> active,
			double usedAllocation) {
		for (TradeOpportunity opportunity : closes) {
			Double allocation = active.remove(opportunity);
			if (allocation != null) {
				usedAllocation -= allocation;
			}
		}
		return Math.abs(usedAllocation) <= EPSILON ? 0.0 : usedAllocation;
	}

	private static Selection selectCandidates(List<TradeOpportunity> candidates,
			Map<TradeOpportunity, Double> active,
			List<SelectedTrade> selected, double usedAllocation, double maxAllocation,
			AllocationPolicy policy) {
		int rejected = 0;
		for (int index = 0; index < candidates.size(); index++) {
			TradeOpportunity candidate = candidates.get(index);
			double remaining = maxAllocation - usedAllocation;
			if (candidate.requestedAllocation() <= remaining + EPSILON) {
				usedAllocation = accept(candidate, candidate.requestedAllocation(), active, selected,
						usedAllocation);
				continue;
			}
			if (policy == AllocationPolicy.PARTIAL_LAST_POSITION && remaining > EPSILON) {
				usedAllocation = accept(candidate, remaining, active, selected, usedAllocation);
				rejected += candidates.size() - index - 1;
			} else {
				rejected += candidates.size() - index;
			}
			break;
		}
		return new Selection(usedAllocation, rejected);
	}

	private static double accept(TradeOpportunity opportunity, double allocation,
			Map<TradeOpportunity, Double> active,
			List<SelectedTrade> selected, double usedAllocation) {
		if (active.putIfAbsent(opportunity, allocation) != null) {
			throw new IllegalStateException("opportunity was already active");
		}
		selected.add(new SelectedTrade(opportunity, allocation));
		return usedAllocation + allocation;
	}

	private static double normalizeSample(double usedAllocation, double maxAllocation) {
		if (Double.isFinite(maxAllocation) && Math.abs(usedAllocation - maxAllocation) <= EPSILON) {
			return maxAllocation;
		}
		return usedAllocation;
	}

	private record PreparedEvents(
			List<List<TradeOpportunity>> opens,
			List<List<TradeOpportunity>> closes,
			int opportunityCount
	) {
	}

	private record Selection(double usedAllocation, int rejectedTrades) {
	}
}
