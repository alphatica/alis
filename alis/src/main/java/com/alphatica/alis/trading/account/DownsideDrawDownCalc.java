package com.alphatica.alis.trading.account;

import static com.alphatica.alis.tools.java.NumberTools.percentChange;

public class DownsideDrawDownCalc {
	private double cash;
	private double maxCash;
	private double maxDD = 0;
	private double currentDD = 0;

	public DownsideDrawDownCalc(double init) {
		cash = init;
		maxCash = cash;
	}

	public void updateState(double cash, double nav) {
		this.cash = cash;
		if (cash > maxCash) {
			maxCash = cash;
		}
		currentDD = Math.min(0, percentChange(maxCash, nav));
		if (currentDD < maxDD) {
			maxDD = currentDD;
		}
	}

	public double getCurrentDownsideDrawdown() {
		return currentDD;
	}

	public double getMaxDownsideDrawdown() {
		return maxDD;
	}

}
