package com.alphatica.alis.trading.account;

/**
 * Return of an account with deposits and withdrawals: cash flows do not count as profit or loss.
 * <p>
 * Every session's return is NAV / (previous NAV + session's net deposits) - 1, i.e. cash flows are treated as made
 * at the start of the session (as the account replay applies them before valuing positions). Session returns are
 * compounded.
 */
public final class TimeWeightedReturn {

	private TimeWeightedReturn() {
	}

	/**
	 * @param nav         NAV at the end of every session
	 * @param netDeposits deposits minus withdrawals of every session
	 * @return cumulative return in percent at the end of every session; a session that starts with nothing invested
	 * (previous NAV + deposits &lt;= 0) has no return
	 */
	public static double[] cumulativePercent(double[] nav, double[] netDeposits) {
		if (nav.length != netDeposits.length) {
			throw new IllegalArgumentException("nav and netDeposits must have the same length");
		}
		double[] result = new double[nav.length];
		double growth = 1;
		double previousNav = 0;
		for (int i = 0; i < nav.length; i++) {
			double invested = previousNav + netDeposits[i];
			if (invested > 0) {
				growth *= nav[i] / invested;
			}
			result[i] = (growth - 1) * 100;
			previousNav = nav[i];
		}
		return result;
	}
}
