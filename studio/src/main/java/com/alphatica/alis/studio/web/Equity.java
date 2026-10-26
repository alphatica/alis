package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.SessionHistory;

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
	 * Time-weighted return, so deposits and withdrawals do not change it; without them (a backtest), the return
	 * relative to the initial capital.
	 */
	static Equity of(SessionHistory sessions) {
		return new Equity(sessions.times().stream().mapToInt(Time::time).toArray(), sessions.nav(),
				sessions.cumulativeReturnPercent());
	}
}
