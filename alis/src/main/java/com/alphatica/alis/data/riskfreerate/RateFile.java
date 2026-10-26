package com.alphatica.alis.data.riskfreerate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.CharacterCodingException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

/**
 * A rates file: a header line, then "date of change,rate in percent" lines (e.g. "2026-03-05,3.75") in increasing
 * order of dates. A rate is in force from its date until the next change.
 */
final class RateFile {
	static final String HEADER = "data_zmiany,stopa_procentowa";
	private static final char BYTE_ORDER_MARK = '﻿';

	/**
	 * @param percent the rate as written in the file, kept so that rewriting the file does not change it
	 */
	record Entry(LocalDate date, String percent) {
		double rate() {
			return Double.parseDouble(percent) / 100;
		}

		String toLine() {
			return date + "," + percent;
		}
	}

	private RateFile() {
	}

	/**
	 * The user's file merged with the built-in rates. The user's rate wins when both have a date; a built-in date
	 * missing from the file is added to it. The file is created when missing and rewritten (in order of dates) when
	 * anything is added.
	 *
	 * @throws IOException when the file is invalid (the message names the line) or cannot be read or written
	 */
	static List<Entry> loadMerged(Path file, List<Entry> builtIn) throws IOException {
		boolean exists = Files.exists(file);
		List<Entry> fromFile = exists ? parse(readLines(file), file.toString()) : List.of();
		List<Entry> merged = merge(fromFile, builtIn);
		if (!exists || merged.size() != fromFile.size()) {
			write(file, merged);
		}
		return merged;
	}

	static List<Entry> builtIn(Country country) {
		try (InputStream stream = RateFile.class.getResourceAsStream(country.fileName())) {
			if (stream == null) {
				throw new IllegalStateException("No built-in risk-free rates for " + country);
			}
			List<Entry> entries = parse(new String(stream.readAllBytes(), UTF_8).lines().toList(),
					"built-in " + country.fileName());
			if (entries.isEmpty()) {
				throw new IllegalStateException("No built-in risk-free rates for " + country);
			}
			return entries;
		} catch (IOException e) {
			throw new IllegalStateException("Invalid built-in risk-free rates for " + country + ": " + e.getMessage(), e);
		}
	}

	/**
	 * @param source shown in error messages, e.g. the file's path
	 * @throws IOException naming the source and the line of the first invalid line
	 */
	static List<Entry> parse(List<String> lines, String source) throws IOException {
		List<Entry> entries = new ArrayList<>();
		for (int i = 0; i < lines.size(); i++) {
			String line = i == 0 ? withoutHeader(lines.get(i)) : lines.get(i).strip();
			if (line.isEmpty()) {
				continue;
			}
			Entry entry = parseLine(line, source, i + 1);
			if (!entries.isEmpty() && !entry.date().isAfter(entries.getLast().date())) {
				throw invalid(source, i + 1, "date " + entry.date() + " is not after the previous date "
						+ entries.getLast().date() + "; dates must be in increasing order");
			}
			entries.add(entry);
		}
		return entries;
	}

	static List<Entry> merge(List<Entry> fromFile, List<Entry> builtIn) {
		Set<LocalDate> dates = new HashSet<>();
		fromFile.forEach(entry -> dates.add(entry.date()));
		List<Entry> missing = builtIn.stream().filter(entry -> !dates.contains(entry.date())).toList();
		if (missing.isEmpty()) {
			return fromFile;
		}
		List<Entry> merged = new ArrayList<>(fromFile);
		merged.addAll(missing);
		merged.sort(Comparator.comparing(Entry::date));
		return merged;
	}

	/** The first line without a byte order mark; empty when it is the header. */
	private static String withoutHeader(String firstLine) {
		String line = firstLine.strip();
		if (!line.isEmpty() && line.charAt(0) == BYTE_ORDER_MARK) {
			line = line.substring(1).strip();
		}
		return line.equals(HEADER) ? "" : line;
	}

	private static Entry parseLine(String line, String source, int lineNumber) throws IOException {
		String[] fields = line.split(",", -1);
		if (fields.length != 2) {
			throw invalid(source, lineNumber, "expected \"date,rate\" (e.g. 2026-03-05,3.75), got \"" + line + "\"");
		}
		LocalDate date;
		try {
			date = LocalDate.parse(fields[0].strip());
		} catch (DateTimeParseException e) {
			throw invalid(source, lineNumber, "invalid date \"" + fields[0].strip() + "\", expected yyyy-mm-dd");
		}
		String percent = fields[1].strip();
		double rate;
		try {
			rate = Double.parseDouble(percent);
		} catch (NumberFormatException e) {
			rate = Double.NaN;
		}
		if (!Double.isFinite(rate)) {
			throw invalid(source, lineNumber, "invalid rate \"" + percent + "\", expected percent with a dot, e.g. 3.75");
		}
		return new Entry(date, percent);
	}

	private static List<String> readLines(Path file) throws IOException {
		try {
			return Files.readAllLines(file, UTF_8);
		} catch (CharacterCodingException e) {
			throw new IOException(file + ": not a UTF-8 text file", e);
		}
	}

	private static void write(Path file, List<Entry> entries) throws IOException {
		List<String> lines = new ArrayList<>();
		lines.add(HEADER);
		entries.forEach(entry -> lines.add(entry.toLine()));
		try {
			Path parent = file.toAbsolutePath().getParent();
			Files.createDirectories(parent);
			Path temporary = parent.resolve(file.getFileName() + ".tmp");
			Files.write(temporary, lines, UTF_8);
			try {
				Files.move(temporary, file, REPLACE_EXISTING, ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temporary, file, REPLACE_EXISTING);
			}
		} catch (IOException e) {
			throw new IOException("Unable to write risk-free rates to " + file + ": " + e.getMessage(), e);
		}
	}

	private static IOException invalid(String source, int lineNumber, String reason) {
		return new IOException(source + ":" + lineNumber + ": " + reason);
	}
}
