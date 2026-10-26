package com.alphatica.alis.trading.account;

import java.math.BigDecimal;

/**
 * Quantities of shares. Outside the account a quantity is a {@code double} number of shares, possibly fractional.
 * Inside the account it is a whole number of units ({@link #UNITS_PER_SHARE} units = 1 share), so adding and
 * subtracting quantities is exact and a fully sold position has exactly zero left.
 */
public final class Quantities {

	/** Smallest quantity the account can hold is 1 / UNITS_PER_SHARE of a share. */
	public static final long UNITS_PER_SHARE = 1_000_000L;
	/** Fraction digits of the smallest quantity. */
	public static final int DECIMAL_PLACES = 6;
	/** Smallest quantity the account can hold, in shares. */
	public static final double MIN_QUANTITY = 1.0 / UNITS_PER_SHARE;
	/** Largest quantity the account can hold, in shares. Above it units would lose precision. */
	public static final double MAX_QUANTITY = 1e12;
	/** Tolerance for floating point errors when rounding down, in units. */
	private static final double ROUND_DOWN_TOLERANCE = 1e-6;

	private Quantities() {
	}

	/**
	 * Nearest whole number of units.
	 */
	public static long toUnits(double shares) {
		checkRange(shares);
		return Math.round(shares * UNITS_PER_SHARE);
	}

	public static double toShares(long units) {
		return (double) units / UNITS_PER_SHARE;
	}

	/**
	 * Quantity rounded to the nearest number the account can hold.
	 */
	public static double normalize(double shares) {
		return toShares(toUnits(shares));
	}

	/**
	 * True when {@code shares} is at least {@link #MIN_QUANTITY}, at most {@link #MAX_QUANTITY} and has no more
	 * fraction digits than the account can hold.
	 */
	public static boolean isValid(double shares) {
		if (!(shares >= MIN_QUANTITY && shares <= MAX_QUANTITY)) {
			return false;
		}
		return Math.abs(normalize(shares) - shares) <= 4 * Math.ulp(shares);
	}

	/**
	 * Largest quantity the account can hold not greater than {@code shares}, at most {@link #MAX_QUANTITY}.
	 * Floating point errors are ignored, e.g. 0.30000000000000004 and 0.29999999999999998 are both 0.3.
	 */
	public static double roundDown(double shares) {
		if (shares <= 0) {
			return 0;
		}
		return toShares((long) Math.floor(Math.min(shares, MAX_QUANTITY) * UNITS_PER_SHARE + ROUND_DOWN_TOLERANCE));
	}

	/**
	 * Quantity without trailing zeros, e.g. "5" or "0.125".
	 */
	public static String format(double shares) {
		return BigDecimal.valueOf(shares).stripTrailingZeros().toPlainString();
	}

	private static void checkRange(double shares) {
		if (!(shares >= 0 && shares <= MAX_QUANTITY)) {
			throw new IllegalArgumentException("Quantity out of range: " + shares);
		}
	}
}
