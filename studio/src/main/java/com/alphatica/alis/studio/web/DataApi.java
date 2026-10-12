package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.loader.DataProcessingException;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.studio.logic.data.MarketsSummary;
import com.alphatica.alis.studio.logic.data.stooq.StooqDataProvider;
import com.alphatica.alis.studio.state.AppState;
import com.alphatica.alis.studio.web.jobs.Job;
import com.alphatica.alis.studio.web.jobs.JobRegistry;
import com.alphatica.alis.studio.web.live.LiveHub;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Data → Stooq: unzipping and loading market data. The data status line of the GUI follows each step.
 */
final class DataApi {
	private static final String PATH = "/api/data";
	private static final String NO_MARKETS = "No data loaded. Download data from Stooq.pl, unzip it and load it.";
	private static final String JOB_TYPE = "data";

	private final JobRegistry jobs;
	private final LiveHub hub;
	private final StateCache<Table> markets = new StateCache<>(DataApi::markets, AppState::getMarketData);

	DataApi(JobRegistry jobs, LiveHub hub) {
		this.jobs = jobs;
		this.hub = hub;
	}

	void register(RoutesConfig routes) {
		TableRoutes.paged(routes, PATH + "/markets", markets);
		routes.get(PATH + "/directories", ctx -> ctx.json(listing(ctx.queryParam("path"))));
		routes.post(PATH + "/unzip", ctx -> start(ctx, this::unzip));
		routes.post(PATH + "/load-pl", ctx -> start(ctx, () -> load(StooqDataProvider::loadPLData)));
		routes.post(PATH + "/load-us", ctx -> start(ctx, () -> load(StooqDataProvider::loadUSData)));
		routes.post(PATH + "/load-pl-directory", ctx -> {
			Path directory = directory(ctx.bodyAsClass(DirectoryRequest.class).directory());
			start(ctx, () -> load(() -> StooqDataProvider.loadPLData(directory)));
		});
	}

	private void start(Context ctx, Runnable operation) {
		Job job = jobs.start(JOB_TYPE, ignored -> operation.run());
		ctx.json(Map.of("jobId", job.id()));
	}

	private void unzip() {
		status("Unzipping new data...");
		try {
			StooqDataProvider.unzipNewData();
		} catch (DataProcessingException e) {
			throw failed(e);
		}
		status("Data unzipped");
	}

	private void load(Supplier<MarketData> loader) {
		status("Loading data...");
		MarketData marketData;
		try {
			marketData = loader.get();
		} catch (DataProcessingException e) {
			throw failed(e);
		}
		AppState.setMarketData(marketData);
		status("Data loaded");
		hub.broadcast("data-loaded", null);
	}

	/**
	 * Shows the failure in the status line. Missing data is the user's to fix; other failures can be reported.
	 */
	private RuntimeException failed(DataProcessingException e) {
		status(failureStatus(e.getReason()));
		return switch (e.getReason()) {
			case NO_DATA -> new ValidationException("No data found");
			case DATA_NOT_FOUND -> new ValidationException("Data not found. Download data from Stooq.pl first");
			case LOAD_FAILED, UNZIP_FAILED, PROCESSING_FAILED -> e;
		};
	}

	private static String failureStatus(DataProcessingException.Reason reason) {
		return switch (reason) {
			case NO_DATA -> "No data found";
			case DATA_NOT_FOUND -> "Data not found";
			case UNZIP_FAILED -> "Unzip failed";
			case LOAD_FAILED, PROCESSING_FAILED -> "Unable to load data";
		};
	}

	private void status(String status) {
		AppState.setDataStatus(status);
		hub.broadcast("data-status", status);
	}

	private static Path directory(String value) {
		if (value == null || value.isBlank()) {
			throw new ValidationException("Enter the Stooq PL data directory");
		}
		Path directory = Path.of(value.trim());
		if (!Files.isDirectory(directory)) {
			throw new ValidationException("Not a directory: " + directory);
		}
		return directory;
	}

	/** Subdirectories of {@code value}, or of the user's home directory when it is empty. Hidden ones are left out. */
	private static DirectoryListing listing(String value) {
		Path directory = value == null || value.isBlank()
				? Path.of(System.getProperty("user.home"))
				: directory(value);
		directory = directory.toAbsolutePath().normalize();
		List<DirectoryEntry> entries = new ArrayList<>();
		try (DirectoryStream<Path> children = Files.newDirectoryStream(directory, DataApi::isVisibleDirectory)) {
			for (Path child : children) {
				entries.add(new DirectoryEntry(child.getFileName().toString(), child.toString()));
			}
		} catch (IOException | UncheckedIOException e) {
			throw new ValidationException("Cannot read directory: " + directory);
		}
		entries.sort(Comparator.comparing(DirectoryEntry::name, String.CASE_INSENSITIVE_ORDER));
		Path parent = directory.getParent();
		return new DirectoryListing(directory.toString(), parent == null ? null : parent.toString(), entries);
	}

	private static boolean isVisibleDirectory(Path path) {
		try {
			return Files.isDirectory(path) && !Files.isHidden(path) && !path.getFileName().toString().startsWith(".");
		} catch (IOException e) {
			return false;
		}
	}

	private static Table markets() {
		MarketData marketData = AppState.getMarketData();
		List<Object[]> rows = marketData == null ? List.of() : MarketsSummary.rows(marketData);
		return new Table(MarketsSummary.COLUMNS, rows, NO_MARKETS);
	}

	record DirectoryRequest(String directory) {
	}

	record DirectoryListing(String path, String parent, List<DirectoryEntry> directories) {
	}

	record DirectoryEntry(String name, String path) {
	}
}
