package com.alphatica.alis.trading.account;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TimeWeightedReturnTest {
	private static final double DELTA = 1e-9;

	@Test
	void shouldCompoundSessionReturns() {
		double[] nav = {1000, 1100, 990};
		double[] deposits = {1000, 0, 0};

		assertArrayEquals(new double[]{0, 10, -1}, TimeWeightedReturn.cumulativePercent(nav, deposits), DELTA);
	}

	@Test
	void shouldIgnoreDepositsAndWithdrawals() {
		// +10%, then a deposit of 900 (NAV 1100 + 900 = 2000) and +10% on that, then a withdrawal of 1200 with no change
		double[] nav = {1000, 1100, 2200, 1000};
		double[] deposits = {1000, 0, 900, -1200};

		assertArrayEquals(new double[]{0, 10, 21, 21}, TimeWeightedReturn.cumulativePercent(nav, deposits), DELTA);
	}

	@Test
	void shouldSkipSessionsWithNothingInvested() {
		// Everything withdrawn, then a new deposit: the empty session has no return
		double[] nav = {1000, 1100, 0, 500, 550};
		double[] deposits = {1000, 0, -1100, 500, 0};

		assertArrayEquals(new double[]{0, 10, 10, 10, 21}, TimeWeightedReturn.cumulativePercent(nav, deposits), DELTA);
	}

	@Test
	void shouldCountFirstSessionResult() {
		// Commission paid on the first day
		double[] nav = {990};
		double[] deposits = {1000};

		assertArrayEquals(new double[]{-1}, TimeWeightedReturn.cumulativePercent(nav, deposits), DELTA);
	}

	@Test
	void shouldRejectDifferentLengths() {
		assertThrows(IllegalArgumentException.class, () -> TimeWeightedReturn.cumulativePercent(new double[1], new double[2]));
	}
}
