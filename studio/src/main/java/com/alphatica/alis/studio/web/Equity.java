package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.SessionHistory;

/**
 * Account value at the close of every market session. Further series (drawdown, benchmark) are meant to be aligned
 * to {@code times}.
 *
 * @param times                   yyyymmdd
 * @param nav                     net asset value: cash + positions at the session's prices
 * @param returnPercent           cumulative return
 * @param closed                  closed equity: cash + positions at entry value, i.e. the effect of closed positions
 * @param closedReturnPercent     cumulative return of the closed equity
 * @param exposurePercent         positions over NAV
 * @param drawdownPercent         0 or negative
 * @param downsideDrawdownPercent 0 or negative, measured from the highest closed equity
 * @param positionCount           open positions, i.e. markets held
 */
public record Equity(int[] times, double[] nav, double[] returnPercent, double[] closed,
					 double[] closedReturnPercent, double[] exposurePercent, double[] drawdownPercent,
					 double[] downsideDrawdownPercent, int[] positionCount) {

	/**
	 * Time-weighted return, so deposits and withdrawals do not change it; without them (a backtest), the return
	 * relative to the initial capital.
	 */
	static Equity of(SessionHistory sessions) {
		return new Equity(sessions.times().stream().mapToInt(Time::time).toArray(), sessions.nav(),
				sessions.cumulativeReturnPercent(), sessions.closedEquity(),
				sessions.cumulativeClosedReturnPercent(), sessions.exposurePercent(), sessions.drawdownPercent(),
				sessions.downsideDrawdownPercent(), sessions.positionCount());
	}
}
