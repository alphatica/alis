package com.alphatica.alis.data.loader.stooq;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class StooqPLDataDirectoryResolver {

	static final String STOOQ_DATA_DIR = "stooq_data";
	private static final String PL_DATA_DIR = "pl";

	private StooqPLDataDirectoryResolver() {
	}

	static Path resolve(String directory) {
		requireSpecified(directory);
		Path selectedDirectory = Path.of(directory).toAbsolutePath().normalize();
		requireExistingDirectory(selectedDirectory);
		return findPLDataDirectory(selectedDirectory);
	}

	private static void requireSpecified(String directory) {
		if (directory == null || directory.isBlank()) {
			throw new IllegalArgumentException("Stooq data directory must be specified");
		}
	}

	private static void requireExistingDirectory(Path directory) {
		if (!Files.isDirectory(directory)) {
			throw new IllegalArgumentException(
					"Stooq data directory does not exist or is not a directory: " + directory);
		}
	}

	private static Path findPLDataDirectory(Path selectedDirectory) {
		return candidates(selectedDirectory).stream()
				.filter(StooqPLDataDirectoryResolver::isPLDataDirectory)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(
						"Directory does not contain unpacked Stooq PL data: " + selectedDirectory));
	}

	private static List<Path> candidates(Path selectedDirectory) {
		return List.of(
				selectedDirectory.resolve(Path.of(STOOQ_DATA_DIR, "data", "daily", PL_DATA_DIR)),
				selectedDirectory.resolve(Path.of("data", "daily", PL_DATA_DIR)),
				selectedDirectory);
	}

	private static boolean isPLDataDirectory(Path candidate) {
		Path fileName = candidate.getFileName();
		return Files.isDirectory(candidate) && fileName != null && PL_DATA_DIR.equals(fileName.toString());
	}
}
