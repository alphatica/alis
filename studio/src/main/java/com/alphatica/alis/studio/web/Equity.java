package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.TimeWeightedReturn;

import java.util.List;

/**
 * Account value at the close of every market session. Further series (drawdown, benchmark) are meant to be aligned
 * to {@code times}.
 *
 * @param times         yyyymmdd
 * @param nav           net asset value: cash + positions at the session's prices
 * @param returnPercent cumulative return
 */
public record Equity(int[] times, double[] nav, double[] returnPercent) {

	/**
	 * Return of the user's portfolio: time-weighted, so deposits and withdrawals do not change it.
	 */
	static Equity timeWeighted(List<Time> times, List<Double> nav, List<Double> netDeposits) {
		double[] navArray = toArray(nav);
		return new Equity(toTimes(times), navArray, TimeWeightedReturn.cumulativePercent(navArray, toArray(netDeposits)));
	}

	/**
	 * Return of a backtest: relative to the initial capital.
	 */
	static Equity fromCapital(List<Time> times, List<Double> nav, double initialCapital) {
		double[] navArray = toArray(nav);
		double[] returnPercent = new double[navArray.length];
		for (int i = 0; i < navArray.length; i++) {
			returnPercent[i] = (navArray[i] / initialCapital - 1) * 100;
		}
		return new Equity(toTimes(times), navArray, returnPercent);
	}

	private static int[] toTimes(List<Time> times) {
		return times.stream().mapToInt(Time::time).toArray();
	}

	private static double[] toArray(List<Double> values) {
		return values.stream().mapToDouble(Double::doubleValue).toArray();
	}
}
