package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.time.Time;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * NAV and closed equity at the end of every session and the session's net deposits, for time-weighted session returns: deposits and
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
	private double[] closed = new double[64];
	private double[] netDeposits = new double[64];
	private double pendingFlow;

	SessionHistory(double startValue) {
		this.startValue = startValue;
	}

	void addFlow(double amount) {
		pendingFlow += amount;
	}

	/**
	 * Values the session: a new one, or the last one again.
	 *
	 * @param closedValue {@link Account#getClosedEquity() closed equity}
	 */
	void record(Time time, double value, double positionsValue, double closedValue) {
		if (!times.isEmpty()) {
			int comparison = time.compareTo(times.getLast());
			if (comparison == 0) {
				revalueLast(value, positionsValue, closedValue);
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
			closed = Arrays.copyOf(closed, index * 2);
			netDeposits = Arrays.copyOf(netDeposits, index * 2);
		}
		times.add(time);
		nav[index] = value;
		positions[index] = positionsValue;
		closed[index] = closedValue;
		netDeposits[index] = pendingFlow;
		pendingFlow = 0;
	}

	/** Values the last session again, e.g. after selling at a delisting; nothing without sessions. */
	void revalueLast(double value, double positionsValue, double closedValue) {
		if (times.isEmpty()) {
			return;
		}
		int last = times.size() - 1;
		nav[last] = value;
		positions[last] = positionsValue;
		closed[last] = closedValue;
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

	/** {@link Account#getClosedEquity() Closed equity} at the end of every session: the effect of closed positions. */
	public double[] closedEquity() {
		return Arrays.copyOf(closed, times.size());
	}

	/**
	 * Account value at the start of every session: the previous NAV (the start value for the first session) plus the
	 * session's net deposits.
	 */
	public double[] startValues() {
		double[] result = new double[times.size()];
		double previous = startValue;
		for (int i = 0; i < result.length; i++) {
			result[i] = previous + netDeposits[i];
			previous = nav[i];
		}
		return result;
	}

	/**
	 * Invested part of the account at the start of every session: positions at the previous valuation over the
	 * {@link #startValues() start value}; 0 for the first session and for a session that starts with nothing.
	 */
	public double[] exposureAtStart() {
		double[] start = startValues();
		double[] result = new double[start.length];
		for (int i = 1; i < result.length; i++) {
			result[i] = start[i] > 0 ? positions[i - 1] / start[i] : 0;
		}
		return result;
	}

	/**
	 * NAV over the previous NAV plus the session's net deposits, minus 1; 0 for a session that starts with nothing
	 * invested.
	 */
	public double[] returns() {
		return returns(nav);
	}

	/** Compounded session returns in percent at the end of every session. */
	public double[] cumulativeReturnPercent() {
		return compound(returns());
	}

	/**
	 * Compounded session returns of the {@link #closedEquity() closed equity} in percent at the end of every session,
	 * time-weighted like {@link #cumulativeReturnPercent()}.
	 */
	public double[] cumulativeClosedReturnPercent() {
		return compound(returns(closed));
	}

	private double[] returns(double[] values) {
		double[] result = new double[times.size()];
		double previous = startValue;
		for (int i = 0; i < result.length; i++) {
			double invested = previous + netDeposits[i];
			result[i] = invested > 0 ? values[i] / invested - 1 : 0;
			previous = values[i];
		}
		return result;
	}

	private static double[] compound(double[] returns) {
		double[] result = returns.clone();
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

	/**
	 * Compounded session returns in percent of the sessions in the year of the last session, i.e. since the end of
	 * the previous year (or since the start when there is no session before); NaN without sessions.
	 */
	public double yearToDateReturnPercent() {
		if (times.isEmpty()) {
			return Double.NaN;
		}
		int year = times.getLast().year();
		double[] returns = returns();
		double growth = 1;
		for (int i = returns.length - 1; i >= 0 && times.get(i).year() == year; i--) {
			growth *= 1 + returns[i];
		}
		return (growth - 1) * 100;
	}
}
