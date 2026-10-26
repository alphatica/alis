package com.alphatica.alis.data.riskfreerate;

import com.alphatica.alis.data.time.Time;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Risk-free rates of every {@link Country}: the built-in rates, overridden and extended by the user's files. A rate
 * is in force from its date until the next change; the first rate also before its date, the last one until any
 * later day.
 */
public final class RiskFreeRates {
	private final Map<Country, Rates> rates;

	private RiskFreeRates(Map<Country, Rates> rates) {
		this.rates = rates;
	}

	/**
	 * Reads the rates file of every country from the directory (e.g. "pl.csv"), creating it from the built-in rates
	 * when missing. A rate in the file wins over the built-in one of the same date; built-in dates missing from the
	 * file are added to it.
	 *
	 * @throws IOException when a file is invalid (the message names the file and the line) or cannot be written
	 */
	public static RiskFreeRates load(Path directory) throws IOException {
		Map<Country, Rates> rates = new EnumMap<>(Country.class);
		for (Country country : Country.values()) {
			Path file = directory.resolve(country.fileName());
			rates.put(country, Rates.of(RateFile.loadMerged(file, RateFile.builtIn(country))));
		}
		return new RiskFreeRates(rates);
	}

	/**
	 * @return annual rate in force on the day, as a fraction (e.g. 0.0375 for 3.75 %)
	 */
	public double freeRate(Country country, Time time) {
		Rates countryRates = rates.get(country);
		return countryRates.rates[countryRates.indexAt(time.time())];
	}

	/**
	 * Days from {@code from} to {@code to} (both inclusive) split at every change of the rate.
	 */
	public List<RatePeriod> periods(Country country, Time from, Time to) {
		if (to.isBefore(from)) {
			throw new IllegalArgumentException("Period ends (" + to + ") before it starts (" + from + ")");
		}
		Rates countryRates = rates.get(country);
		List<RatePeriod> periods = new ArrayList<>();
		int index = countryRates.indexAt(from.time());
		Time start = from;
		while (index + 1 < countryRates.dates.length && countryRates.dates[index + 1] <= to.time()) {
			Time change = new Time(countryRates.dates[index + 1]);
			periods.add(new RatePeriod(start, Time.of(change.toLocalDate().minusDays(1)), countryRates.rates[index]));
			start = change;
			index++;
		}
		periods.add(new RatePeriod(start, to, countryRates.rates[index]));
		return periods;
	}

	/** Dates (yyyymmdd) of changes in increasing order; an entry repeating the previous rate is not a change. */
	private record Rates(int[] dates, double[] rates) {

		static Rates of(List<RateFile.Entry> entries) {
			int[] dates = new int[entries.size()];
			double[] rates = new double[entries.size()];
			int count = 0;
			for (RateFile.Entry entry : entries) {
				double rate = entry.rate();
				if (count == 0 || rate != rates[count - 1]) {
					dates[count] = Time.of(entry.date()).time();
					rates[count] = rate;
					count++;
				}
			}
			return new Rates(Arrays.copyOf(dates, count), Arrays.copyOf(rates, count));
		}

		/** Index of the rate in force on the day: the last change on or before it, else the first one. */
		int indexAt(int date) {
			int index = Arrays.binarySearch(dates, date);
			return index >= 0 ? index : Math.max(-index - 2, 0);
		}
	}
}
