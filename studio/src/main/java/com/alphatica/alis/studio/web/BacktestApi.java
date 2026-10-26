package com.alphatica.alis.studio.web;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.logic.stats.Stat;
import com.alphatica.alis.studio.logic.table.Column;
import com.alphatica.alis.studio.logic.trading.backtest.BacktestResult;
import com.alphatica.alis.studio.logic.trading.backtest.BacktestRunner;
import com.alphatica.alis.studio.logic.trading.backtest.BacktestSettings;
import com.alphatica.alis.studio.logic.trading.backtest.BacktestStats;
import com.alphatica.alis.studio.strategy.StrategyList;
import com.alphatica.alis.studio.web.jobs.Job;
import com.alphatica.alis.studio.web.jobs.JobChannel;
import com.alphatica.alis.studio.web.jobs.JobRegistry;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.PositionPricesRecord;
import com.alphatica.alis.trading.account.RecentDrawdownChecker;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.BarExecutedConsumer;
import com.alphatica.alis.trading.strategy.random.RandomStrategyComparator;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Strategies → Backtest: runs a strategy over a time range and streams its orders and trades, then shows stats and
 * equity. The last result can be exported as CSV, checked for drawdown and compared with a random strategy.
 */
final class BacktestApi {
	private static final String PATH = "/api/backtest";
	private static final String JOB_TYPE = "backtest";
	private static final String RANDOM_JOB_TYPE = "backtest-random";
	private static final int MAX_ITERATIONS = 1_000_000;

	private static final List<Column> ORDERS = List.of(Column.date("Time"), Column.text("Market"), Column.text("Order"),
			Column.text("Size"), Column.number("Priority", 2));
	private static final List<Column> TRADES = List.of(Column.text("Market"), Column.date("Entry time"),
			Column.date("Exit time"), Column.number("Entry price", 2), Column.number("Exit price", 2),
			Column.quantity("Quantity"), Column.percent("Profit %", 2), Column.number("Profit (cash)", 2),
			Column.number("Entry efficiency", 2), Column.number("Exit efficiency", 2));

	private final JobRegistry jobs;
	/** Result of the last finished backtest, for export, drawdown check and comparison with random. */
	private volatile LastRun lastRun;

	BacktestApi(JobRegistry jobs) {
		this.jobs = jobs;
	}

	void register(RoutesConfig routes) {
		routes.get(PATH + "/options", ctx -> ctx.json(new BacktestOptions(List.copyOf(StrategyList.STRATEGIES.keySet()),
				RunDefaults.current(), ORDERS, TRADES)));
		routes.post(PATH, this::start);
		routes.get(PATH + "/trades.csv", this::exportTrades);
		routes.post(PATH + "/drawdown", ctx -> ctx.json(Map.of("lines",
				RecentDrawdownChecker.checkDrawdown(requireLastRun().result().nav()))));
		routes.post(PATH + "/compare-random", this::compareWithRandom);
	}

	private void start(Context ctx) {
		BacktestRequest request = ctx.bodyAsClass(BacktestRequest.class);
		MarketData marketData = Inputs.marketData();
		BacktestSettings settings = Inputs.settings(request);
		lastRun = null;
		Job job = jobs.start(JOB_TYPE, running -> run(running.channel(), settings, marketData));
		ctx.json(Map.of("jobId", job.id()));
	}

	private void run(JobChannel channel, BacktestSettings settings, MarketData marketData) throws AccountActionException {
		int totalBars = (int) marketData.getTimes().stream()
				.filter(time -> !time.isBefore(settings.timeStart()) && !time.isAfter(settings.timeEnd()))
				.count();
		LiveOutput output = new LiveOutput(channel, totalBars);
		BacktestResult result = BacktestRunner.run(settings, marketData, output);
		// Positions closed at the end of the backtest
		output.sendNewTrades(result.account());
		lastRun = new LastRun(result, settings, marketData);
		channel.replace("result", new BacktestView(BacktestStats.of(result),
				Equity.fromCapital(result.times(), result.nav(), settings.initialCapital())));
	}

	private void exportTrades(Context ctx) {
		List<AccountAction> actions = requireLastRun().result().account().getAccountHistory().getActions();
		String csv = actions.stream().map(AccountAction::toCsv).collect(Collectors.joining("\n", "", "\n"));
		ctx.contentType("text/csv; charset=utf-8");
		ctx.header("Content-Disposition", "attachment; filename=\"backtest-trades.csv\"");
		ctx.result(csv);
	}

	private void compareWithRandom(Context ctx) {
		LastRun run = requireLastRun();
		int iterations = (int) Inputs.wholeNumber(ctx.bodyAsClass(CompareRequest.class).iterations(),
				"number of iterations", 1, MAX_ITERATIONS);
		BacktestSettings settings = run.settings();
		Job job = jobs.start(RANDOM_JOB_TYPE, running -> {
			RandomStrategyComparator comparator = new RandomStrategyComparator(
					line -> running.channel().append("lines", line), iterations, settings.initialCapital(),
					settings.commissionRate(), settings.timeStart(), settings.timeEnd(), run.marketData());
			running.onCancel(comparator::stop);
			comparator.compare(run.result().account());
			if (running.isCancelRequested()) {
				running.channel().append("lines", "Stopped.");
			}
		});
		ctx.json(Map.of("jobId", job.id()));
	}

	private LastRun requireLastRun() {
		LastRun run = lastRun;
		if (run == null) {
			throw new ValidationException("Run a backtest first");
		}
		return run;
	}

	/**
	 * Sends the orders and the newly closed trades of every session, and the progress.
	 */
	private static final class LiveOutput implements BarExecutedConsumer {
		private final JobChannel channel;
		private final int totalBars;
		private int bars;
		private int sentTrades;

		LiveOutput(JobChannel channel, int totalBars) {
			this.channel = channel;
			this.totalBars = totalBars;
		}

		@Override
		public void execute(Time time, Account account, List<Order> orders) {
			for (Order order : orders) {
				channel.append("orders", new Object[]{time.time(), order.market().name(), order.direction().toString(),
						order.formatSize(), order.priority()});
			}
			sendNewTrades(account);
			bars++;
			channel.replace("progress", new Progress(time.toString(), Math.min(bars, totalBars), totalBars));
		}

		void sendNewTrades(Account account) {
			List<PositionPricesRecord> records = account.getClosedPricesRecords();
			for (; sentTrades < records.size(); sentTrades++) {
				PositionPricesRecord r = records.get(sentTrades);
				channel.append("trades", new Object[]{r.marketName().toString(), r.entryTime().time(),
						r.exitTime() == null ? null : r.exitTime().time(), r.entry(), r.exit(), r.quantity(),
						r.getProfitPercent(), r.getProfitCash(), r.getEntryEfficiency(), r.getExitEfficiency()});
			}
		}
	}

	private record LastRun(BacktestResult result, BacktestSettings settings, MarketData marketData) {
	}

	record BacktestOptions(List<String> strategies, RunDefaults defaults, List<Column> orders, List<Column> trades) {
	}

	record BacktestRequest(String strategy, String startTime, String endTime, String commissionRate,
	                       String initialCapital) implements Inputs.RunForm {
	}

	record CompareRequest(String iterations) {
	}

	/**
	 * @param bar number of sessions done, out of {@code bars}
	 */
	record Progress(String time, int bar, int bars) {
	}

	record BacktestView(List<Stat> stats, Equity equity) {
	}
}
