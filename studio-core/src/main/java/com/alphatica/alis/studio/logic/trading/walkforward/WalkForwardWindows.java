package com.alphatica.alis.studio.logic.trading.walkforward;

import com.alphatica.alis.data.time.Time;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Windows of a walk-forward: test windows of {@code testMonths} one after another from the start to the end (the last
 * one cut at the end), each optimized on the {@code trainingMonths} just before it. The training reaches before the
 * start.
 */
public final class WalkForwardWindows {
	/** Longest window: 50 years. */
	public static final int MAX_MONTHS = 600;

	private WalkForwardWindows() {
	}

	/**
	 * @param number   from 1
	 * @param trainingEnd last day of the training, the day before the test starts
	 * @param testEnd  last day of the test, inclusive
	 */
	public record Window(int number, Time trainingStart, Time trainingEnd, Time testStart, Time testEnd) {
	}

	/**
	 * @param start first day of the first test window
	 * @param end   last day of the last test window
	 */
	public static List<Window> of(Time start, Time end, int trainingMonths, int testMonths) {
		checkMonths(trainingMonths, "Training");
		checkMonths(testMonths, "Test");
		if (start.isAfter(end)) {
			throw new IllegalArgumentException("Start " + start + " after end " + end);
		}
		LocalDate first = start.toLocalDate();
		LocalDate last = end.toLocalDate();
		List<Window> windows = new ArrayList<>();
		for (int i = 0; ; i++) {
			LocalDate testStart = first.plusMonths((long) i * testMonths);
			if (testStart.isAfter(last)) {
				return windows;
			}
			LocalDate nextStart = first.plusMonths((long) (i + 1) * testMonths);
			LocalDate testEnd = nextStart.minusDays(1).isAfter(last) ? last : nextStart.minusDays(1);
			windows.add(new Window(i + 1, Time.of(testStart.minusMonths(trainingMonths)), Time.of(testStart.minusDays(1)),
					Time.of(testStart), Time.of(testEnd)));
		}
	}

	private static void checkMonths(int months, String window) {
		if (months < 1 || months > MAX_MONTHS) {
			throw new IllegalArgumentException(window + " window must be from 1 to " + MAX_MONTHS + " months: " + months);
		}
	}
}
