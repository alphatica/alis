package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.logic.trading.backtest.BacktestSettings;
import com.alphatica.alis.studio.state.AppState;
import com.alphatica.alis.studio.strategy.StrategyList;
import com.alphatica.alis.trading.strategy.Strategy;

import java.time.DateTimeException;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Parsing of form fields. Invalid input ends as {@link ValidationException}, shown to the user.
 */
final class Inputs {

	private Inputs() {
	}

	/**
	 * Accepts 20240510 as well as 2024-05-10.
	 */
	static Time time(String value, String field) {
		String digits = text(value, field).replace("-", "").replace(" ", "").replace("_", "");
		try {
			Time time = new Time(Integer.parseInt(digits));
			time.toLocalDate();
			return time;
		} catch (NumberFormatException | DateTimeException e) {
			throw new ValidationException("Invalid " + field + ": " + value);
		}
	}

	/**
	 * Non-negative number; accepts a comma as the decimal separator and spaces between digits.
	 */
	static double number(String value, String field) {
		double number;
		try {
			number = Double.parseDouble(text(value, field).replace(",", ".").replace(" ", "").replace("_", ""));
		} catch (NumberFormatException e) {
			throw new ValidationException("Invalid " + field + ": " + value);
		}
		if (!Double.isFinite(number) || number < 0) {
			throw new ValidationException("Invalid " + field + ": " + value);
		}
		return number;
	}

	/**
	 * Whole number from {@code min} to {@code max}.
	 */
	static long wholeNumber(String value, String field, long min, long max) {
		double number = number(value, field);
		if (number < min || number > max || number != Math.rint(number)) {
			String range = max >= Integer.MAX_VALUE ? "of at least " + min : "from " + min + " to " + max;
			throw new ValidationException("The " + field + " must be a whole number " + range);
		}
		return (long) number;
	}

	static String text(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new ValidationException("Enter the " + field);
		}
		return value.trim();
	}

	/**
	 * One of the options offered by the GUI, by its name.
	 */
	static <T> T option(Map<String, T> options, String name, String field) {
		return Optional.ofNullable(name).map(options::get)
				.orElseThrow(() -> new ValidationException("Unknown " + field + ": " + name));
	}

	static Supplier<Strategy> strategy(String name) {
		return option(StrategyList.STRATEGIES, name, "strategy");
	}

	/**
	 * The loaded market data; most actions need it.
	 */
	static MarketData marketData() {
		MarketData marketData = AppState.getMarketData();
		if (marketData == null) {
			throw new ValidationException("No data loaded. Load market data first");
		}
		return marketData;
	}

	static BacktestSettings settings(RunForm form) {
		Time startTime = time(form.startTime(), "start time");
		Time endTime = time(form.endTime(), "end time");
		if (!startTime.isBefore(endTime)) {
			throw new ValidationException("Start time is not before end time");
		}
		double commissionRate = number(form.commissionRate(), "commission rate");
		double initialCapital = number(form.initialCapital(), "initial capital");
		if (initialCapital <= 0) {
			throw new ValidationException("Initial capital too small");
		}
		return new BacktestSettings(startTime, endTime, commissionRate, initialCapital, strategy(form.strategy()));
	}

	/**
	 * Fields of a strategy run, the same in the backtest and the optimization forms.
	 */
	interface RunForm {
		String strategy();

		String startTime();

		String endTime();

		String commissionRate();

		String initialCapital();
	}
}
