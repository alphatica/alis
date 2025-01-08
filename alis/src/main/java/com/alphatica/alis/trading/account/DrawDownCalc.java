package com.alphatica.alis.trading.account;

public class DrawDownCalc {
	private double dd = 1.0;
	private double maxDd = 1.0;
	private double nav;

	public void changeCash(double change) {
		nav += change;
	}

	public void updateNav(double newNav) {
		if (nav == 0) {
			nav = newNav;
			return;
		}
		double change = newNav / nav;
		nav = newNav;
		dd *= change;
		dd = Math.min(1, dd);
		maxDd = Math.min(dd, maxDd);
	}

	public double getCurrentDD() {
		return -100 * (1 - dd);
	}

	public double getMaxDD() {
		return -100 * (1 - maxDd);
	}
}
