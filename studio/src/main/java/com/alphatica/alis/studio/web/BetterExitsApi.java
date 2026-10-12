package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.studio.dao.AccountActionCSVFacade;
import com.alphatica.alis.studio.logic.table.Column;
import com.alphatica.alis.studio.web.jobs.Job;
import com.alphatica.alis.studio.web.jobs.JobRegistry;
import com.alphatica.alis.tools.java.TopResults;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.AccountReplayer;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.scorer.AccountScorer;
import com.alphatica.alis.trading.datamining.BetterExitsSearch;
import com.alphatica.alis.trading.datamining.betterexits.ExitFinderResult;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.alphatica.alis.studio.logic.options.StudioOptions.EXIT_SCORERS;

/**
 * Analysis → Find better exits: replays an account log loaded from a CSV file
 * with alternative exit rules and streams the best 100 results found so far and the number of iterations done.
 * The loaded log is kept here, so it survives reloading the page.
 */
final class BetterExitsApi {
	private static final String PATH = "/api/better-exits";
	private static final String JOB_TYPE = "better-exits";
	private static final int TOP_RESULTS = 100;
	private static final String DEFAULT_COMMISSION_RATE = "0.01";

	private static final List<Column> RESULTS = List.of(Column.number("Score", 1), Column.number("Early exits", 0),
			Column.text("Name"), Column.text("Description"));

	private final JobRegistry jobs;
	private volatile LoadedFile loadedFile;

	BetterExitsApi(JobRegistry jobs) {
		this.jobs = jobs;
	}

	void register(RoutesConfig routes) {
		routes.get(PATH + "/options", ctx -> ctx.json(options()));
		routes.post(PATH + "/file", this::loadFile);
		routes.post(PATH + "/score", this::score);
		routes.post(PATH, this::start);
	}

	private BetterExitsOptions options() {
		LoadedFile file = loadedFile;
		return new BetterExitsOptions(List.copyOf(EXIT_SCORERS.keySet()), DEFAULT_COMMISSION_RATE, file == null ? null : file.info(), RESULTS);
	}

	/**
	 * Body: the file's content. Read as a stream, so large logs are not cut by the request size limit.
	 */
	private void loadFile(Context ctx) throws IOException {
		String name = Inputs.text(ctx.queryParam("name"), "file name");
		List<AccountAction> actions;
		try (Reader reader = new InputStreamReader(ctx.bodyInputStream(), StandardCharsets.UTF_8)) {
			actions = AccountActionCSVFacade.readActions(reader);
		} catch (IllegalArgumentException | IllegalStateException e) {
			throw new ValidationException(e.getMessage());
		}
		if (actions.isEmpty()) {
			throw new ValidationException("No account log in the file");
		}
		LoadedFile file = new LoadedFile(name, List.copyOf(actions));
		loadedFile = file;
		ctx.json(file.info());
	}

	/** Score of the loaded log as it is, with the chosen scorer and commission rate. */
	private void score(Context ctx) {
		SearchRequest request = ctx.bodyAsClass(SearchRequest.class);
		Settings settings = settings(request);
		Account account;
		try {
			account = AccountReplayer.replay(settings.marketData(), settings.file().actions(), settings.commissionRate());
		} catch (AccountActionException e) {
			throw new ValidationException(e.getMessage());
		}
		double score = settings.scorer().get().score(account, null);
		ctx.json(Map.of("score", score));
	}

	private void start(Context ctx) {
		Settings settings = settings(ctx.bodyAsClass(SearchRequest.class));
		BetterExitsSearch search = new BetterExitsSearch(settings.marketData(), settings.file().actions(),
				settings.scorer(), settings.commissionRate());
		Job job = jobs.start(JOB_TYPE, running -> run(running, search));
		ctx.json(Map.of("jobId", job.id()));
	}

	private static void run(Job job, BetterExitsSearch search) {
		TopResults<Result> topResults = new TopResults<>(TOP_RESULTS,
				Comparator.<Result>comparingDouble(Result::score).reversed().thenComparing(Result::name), Result::name);
		job.onCancel(search::stop);
		job.channel().replace("iterations", 0);
		search.run((result, iterationsDone) -> {
			if (topResults.add(Result.of(result))) {
				job.channel().replace("results", topResults.list());
			}
			job.channel().replace("iterations", iterationsDone);
		});
	}

	private Settings settings(SearchRequest request) {
		MarketData marketData = Inputs.marketData();
		LoadedFile file = loadedFile;
		if (file == null) {
			throw new ValidationException("Load an account log file first");
		}
		double commissionRate = Inputs.number(request.commissionRate(), "commission rate");
		if (commissionRate >= 1) {
			throw new ValidationException("Commission rate must be lower than 1");
		}
		return new Settings(marketData, file, commissionRate, Inputs.option(EXIT_SCORERS, request.scorer(), "result scorer"));
	}

	private record Settings(MarketData marketData, LoadedFile file, double commissionRate,
	                        Supplier<AccountScorer> scorer) {
	}

	private record LoadedFile(String name, List<AccountAction> actions) {
		FileInfo info() {
			return new FileInfo(name, actions.size());
		}
	}

	record FileInfo(String name, int actions) {
	}

	/**
	 * One row of the results table; the name is unique among the top results.
	 *
	 * @param cells values of the {@link #RESULTS} columns
	 */
	record Result(double score, String name, List<Object> cells) {

		static Result of(ExitFinderResult result) {
			return new Result(result.score(), result.name(),
					Arrays.asList(result.score(), result.trades(), result.name(), result.description()));
		}
	}

	/** @param file the loaded account log, null until one is loaded */
	record BetterExitsOptions(List<String> scorers, String defaultCommissionRate, FileInfo file,
	                          List<Column> results) {
	}

	record SearchRequest(String commissionRate, String scorer) {
	}
}
