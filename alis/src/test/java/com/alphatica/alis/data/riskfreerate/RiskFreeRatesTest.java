package com.alphatica.alis.data.riskfreerate;

import com.alphatica.alis.data.time.Time;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RiskFreeRatesTest {
	private static final double DELTA = 1e-12;

	@TempDir
	Path directory;

	@Test
	void shouldCreateFileFromBuiltInRates() throws IOException {
		RiskFreeRates rates = RiskFreeRates.load(directory.resolve("rf"));

		List<String> lines = Files.readAllLines(directory.resolve("rf").resolve("pl.csv"), UTF_8);
		assertEquals(RateFile.HEADER, lines.getFirst());
		assertEquals(builtInLines(), lines.subList(1, lines.size()));
		assertEquals(0.0375, rates.freeRate(Country.PL, new Time(20260305)), DELTA);
		assertEquals(0.04, rates.freeRate(Country.PL, new Time(20260304)), DELTA);
	}

	@Test
	void shouldUseFirstRateBeforeItAndLastRateAfterIt() throws IOException {
		RiskFreeRates rates = RiskFreeRates.load(directory);

		assertEquals(0.24, rates.freeRate(Country.PL, new Time(19910101)), DELTA);
		assertEquals(0.0375, rates.freeRate(Country.PL, new Time(20991231)), DELTA);
	}

	@Test
	void shouldPreferUsersRateAndUseUsersDates() throws IOException {
		List<String> lines = new ArrayList<>(builtInLines());
		lines.set(lines.indexOf("2026-03-05,3.75"), "2026-03-05,3.5");
		lines.add("2026-09-01,3.25");
		List<String> content = withHeader(lines);
		write(content);

		RiskFreeRates rates = RiskFreeRates.load(directory);

		assertEquals(0.035, rates.freeRate(Country.PL, new Time(20260305)), DELTA);
		assertEquals(0.035, rates.freeRate(Country.PL, new Time(20260831)), DELTA);
		assertEquals(0.0325, rates.freeRate(Country.PL, new Time(20260901)), DELTA);
		// Nothing to add: the file is unchanged
		assertEquals(content, read());
	}

	@Test
	void shouldInsertMissingBuiltInRatesInOrderOfDates() throws IOException {
		write(List.of(RateFile.HEADER, "2000-01-01,99", "2030-01-01,1.5"));

		RiskFreeRates rates = RiskFreeRates.load(directory);

		List<String> expected = new ArrayList<>(builtInLines());
		expected.add(expected.indexOf("2000-02-24,17.50"), "2000-01-01,99");
		expected.add("2030-01-01,1.5");
		assertEquals(withHeader(expected), read());
		assertEquals(0.99, rates.freeRate(Country.PL, new Time(20000101)), DELTA);
		assertEquals(0.015, rates.freeRate(Country.PL, new Time(20300101)), DELTA);
	}

	@Test
	void shouldAcceptFileWithoutHeaderOrWithByteOrderMark() throws IOException {
		write(List.of("2030-01-01,1.5"));
		assertEquals(0.015, RiskFreeRates.load(directory).freeRate(Country.PL, new Time(20300101)), DELTA);

		write(List.of("﻿" + RateFile.HEADER, "2030-01-01,1.25"));
		assertEquals(0.0125, RiskFreeRates.load(directory).freeRate(Country.PL, new Time(20300101)), DELTA);
	}

	@Test
	void shouldRejectInvalidLinesNamingTheLine() throws IOException {
		assertInvalid(List.of(RateFile.HEADER, "2030-01-01,1,5"), "pl.csv:2: expected \"date,rate\"");
		assertInvalid(List.of(RateFile.HEADER, "2030-1-1,1.5"), "pl.csv:2: invalid date");
		assertInvalid(List.of(RateFile.HEADER, "2030-01-01,abc"), "pl.csv:2: invalid rate");
		assertInvalid(List.of(RateFile.HEADER, "2030-01-01,NaN"), "pl.csv:2: invalid rate");
		assertInvalid(List.of(RateFile.HEADER, "2030-01-01,1.5", "2029-01-01,1.5"), "pl.csv:3: date 2029-01-01");
		assertInvalid(List.of(RateFile.HEADER, "2030-01-01,1.5", "2030-01-01,2"), "pl.csv:3: date 2030-01-01");
	}

	@Test
	void shouldFailWhenFileCannotBeWritten() throws IOException {
		Path notDirectory = directory.resolve("file");
		Files.writeString(notDirectory, "");

		IOException e = assertThrows(IOException.class, () -> RiskFreeRates.load(notDirectory));

		assertTrue(e.getMessage().startsWith("Unable to write risk-free rates"), e.getMessage());
	}

	@Test
	void shouldSplitPeriodsAtChangesOfRate() throws IOException {
		RiskFreeRates rates = RiskFreeRates.load(directory);

		assertEquals(List.of(
						new RatePeriod(new Time(20251120), new Time(20251203), 0.0425),
						new RatePeriod(new Time(20251204), new Time(20260304), 0.04),
						new RatePeriod(new Time(20260305), new Time(20270101), 0.0375)),
				rates.periods(Country.PL, new Time(20251120), new Time(20270101)));
		assertEquals(List.of(new RatePeriod(new Time(20260101), new Time(20260201), 0.04)),
				rates.periods(Country.PL, new Time(20260101), new Time(20260201)));
		assertEquals(List.of(
						new RatePeriod(new Time(19950101), new Time(19980422), 0.24),
						new RatePeriod(new Time(19980423), new Time(19980423), 0.23)),
				rates.periods(Country.PL, new Time(19950101), new Time(19980423)));
		assertThrows(IllegalArgumentException.class,
				() -> rates.periods(Country.PL, new Time(20260102), new Time(20260101)));
	}

	@Test
	void shouldNotSplitPeriodAtUnchangedRate() throws IOException {
		write(List.of(RateFile.HEADER, "2030-01-01,3.75", "2031-01-01,3.75"));

		RiskFreeRates rates = RiskFreeRates.load(directory);

		assertEquals(List.of(new RatePeriod(new Time(20260305), new Time(20320101), 0.0375)),
				rates.periods(Country.PL, new Time(20260305), new Time(20320101)));
	}

	private void assertInvalid(List<String> lines, String messagePart) throws IOException {
		write(lines);
		IOException e = assertThrows(IOException.class, () -> RiskFreeRates.load(directory));
		assertTrue(e.getMessage().contains(messagePart), e.getMessage());
	}

	private static List<String> builtInLines() {
		return RateFile.builtIn(Country.PL).stream().map(RateFile.Entry::toLine).toList();
	}

	private static List<String> withHeader(List<String> lines) {
		List<String> result = new ArrayList<>();
		result.add(RateFile.HEADER);
		result.addAll(lines);
		return result;
	}

	private void write(List<String> lines) throws IOException {
		Files.write(directory.resolve("pl.csv"), lines, UTF_8);
	}

	private List<String> read() throws IOException {
		return Files.readAllLines(directory.resolve("pl.csv"), UTF_8);
	}
}
