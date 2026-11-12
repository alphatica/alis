package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.time.Time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * NAV at the end of every session and the session's net deposits, for time-weighted session returns: deposits and
 * withdrawals are not profit. Closing positions and delistings after a session's valuation count in that session.
 * <p>
 * Cash flows are counted in the next valued session, i.e. as made at its start (as the account replay applies them
 * before valuing positions). Flows after the last valuation do not change any return and are left out.
 */
public final class SessionHistory {
	private final double startValue;
	private final List<Time> times = new ArrayList<>();
	private double[] nav = new double[64];
	private double[] positions = new double[64];
	private double[] netDeposits = new double[64];
	private double pendingFlow;

	SessionHistory(double startValue) {
		this.startValue = startValue;
	}

	void addFlow(double amount) {
		pendingFlow += amount;
	}

	/** Values the session: a new one, or the last one again. */
	void record(Time time, double value) {
		record(time, value, 0);
	}

	void record(Time time, double value, double positionsValue) {
		if (!times.isEmpty()) {
			int comparison = time.compareTo(times.getLast());
			if (comparison == 0) {
				revalueLast(value, positionsValue);
				return;
			}
			if (comparison < 0) {
				throw new IllegalArgumentException("Session " + time + " before " + times.getLast());
			}
		}
		int index = times.size();
		if (index == nav.length) {
			nav = Arrays.copyOf(nav, index * 2);
			positions = Arrays.copyOf(positions, index * 2);
			netDeposits = Arrays.copyOf(netDeposits, index * 2);
		}
		times.add(time);
		nav[index] = value;
		positions[index] = positionsValue;
		netDeposits[index] = pendingFlow;
		pendingFlow = 0;
	}

	/** Values the last session again, e.g. after selling at a delisting; nothing without sessions. */
	void revalueLast(double value) {
		revalueLast(value, 0);
	}

	void revalueLast(double value, double positionsValue) {
		if (times.isEmpty()) {
			return;
		}
		int last = times.size() - 1;
		nav[last] = value;
		positions[last] = positionsValue;
		netDeposits[last] += pendingFlow;
		pendingFlow = 0;
	}

	/** Valued sessions, in order. */
	public List<Time> times() {
		return Collections.unmodifiableList(times);
	}

	/** NAV at the end of every session. */
	public double[] nav() {
		return Arrays.copyOf(nav, times.size());
	}

	public double[] exposure() {
		double[] result = new double[times.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = nav[i] > 0 ? positions[i] / nav[i] : 0;
		}
		return result;
	}

	/**
	 * NAV over the previous NAV plus the session's net deposits, minus 1; 0 for a session that starts with nothing
	 * invested.
	 */
	public double[] returns() {
		double[] result = new double[times.size()];
		double previous = startValue;
		for (int i = 0; i < result.length; i++) {
			double invested = previous + netDeposits[i];
			result[i] = invested > 0 ? nav[i] / invested - 1 : 0;
			previous = nav[i];
		}
		return result;
	}

	/** Compounded session returns in percent at the end of every session. */
	public double[] cumulativeReturnPercent() {
		double[] result = returns();
		double growth = 1;
		for (int i = 0; i < result.length; i++) {
			growth *= 1 + result[i];
			result[i] = (growth - 1) * 100;
		}
		return result;
	}

	/** Compounded session returns in percent at the end of the last session; NaN without sessions. */
	public double totalReturnPercent() {
		double[] cumulative = cumulativeReturnPercent();
		return cumulative.length == 0 ? Double.NaN : cumulative[cumulative.length - 1];
	}
}
