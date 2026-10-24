package com.alphatica.alis.trading.account;

import static com.alphatica.alis.tools.java.NumberTools.percentChange;

/**
 * Drawdown of NAV measured from the highest closed equity (cash + entry value of open positions).
 * Giving back unrealized profit is not a drawdown, and unrealized profit of one position covers losses of others.
 * Closed profits raise the reference, so later losses are not covered by them.
 * Deposits and withdrawals are not drawdown: see {@link #changeCash}.
 */
public class DownsideDrawdownCalc {
	private double maxClosedEquity;
	private double maxDD = 0;
	private double currentDD = 0;

	public DownsideDrawdownCalc(double init) {
		maxClosedEquity = init;
	}

	public void updateState(double closedEquity, double nav) {
		if (closedEquity > maxClosedEquity) {
			maxClosedEquity = closedEquity;
		}
		if (maxClosedEquity <= 0) {
			currentDD = 0;
			return;
		}
		currentDD = Math.min(0, percentChange(maxClosedEquity, nav));
		if (currentDD < maxDD) {
			maxDD = currentDD;
		}
	}

	/**
	 * Call when cash is deposited (positive amount) or withdrawn (negative amount), then call {@link #updateState}.
	 * Closed losses keep their percentage (reference scales with closed equity), open losses keep their amount.
	 */
	public void changeCash(double closedEquityBefore, double amount) {
		double closedEquityAfter = closedEquityBefore + amount;
		if (closedEquityBefore > 0) {
			maxClosedEquity *= closedEquityAfter / closedEquityBefore;
		} else {
			maxClosedEquity = closedEquityAfter;
		}
	}

	public double getCurrentDownsideDrawdown() {
		return currentDD;
	}

	public double getMaxDownsideDrawdown() {
		return maxDD;
	}

}
