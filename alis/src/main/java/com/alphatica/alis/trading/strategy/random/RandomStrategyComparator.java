package com.alphatica.alis.trading.strategy.random;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.AccountReplayer;
import com.alphatica.alis.trading.account.PositionHistoryRecord;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.strategy.StrategyExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * Compares a backtest with random trading that has the same number of trades, the same holding lengths and the same
 * average exposure, and counts how often random trading ends with a higher account value.
 * <p>
 * Holding lengths are drawn from the trades of the backtest. The position size is the backtest's average exposure
 * divided by its average number of open positions. The entry probability is searched for until random trading makes
 * about as many trades as the backtest.
 */
public class RandomStrategyComparator {
	private static final int CALIBRATION_RUNS = 10;
	private static final int MAX_CALIBRATION_STEPS = 30;
	private static final double INITIAL_PROBABILITY = 0.01;
	private static final double MAX_PROBABILITY = 1.0;
	private static final double MAX_POSITION_SIZE_PERCENT = 100.0;

	private final AtomicBoolean isStopped;
	private final Consumer<String> stringConsumer;
	private final int iterations;
	private final double initialCash;
	private final double commissionRate;
	private final Time startTime;
	private final Time endTime;
	private final MarketData marketData;

	public RandomStrategyComparator(Consumer<String> stringConsumer, int iterations, double initialCash, double commissionRate, Time startTime, Time endTime, MarketData marketData) {
		this.isStopped = new AtomicBoolean(false);
		this.stringConsumer = stringConsumer;
		this.iterations = iterations;
		this.initialCash = initialCash;
		this.commissionRate = commissionRate;
		this.startTime = startTime;
		this.endTime = endTime;
		this.marketData = marketData;
	}

	public void compare(Account baseAccount) {
		int[] lengths = tradeLengths(baseAccount);
		if (lengths.length == 0) {
			stringConsumer.accept("The backtest has no trades, nothing to compare.");
			return;
		}
		Profile base;
		try {
			base = profileOf(baseAccount, lengths);
		} catch (AccountActionException e) {
			stringConsumer.accept("Error: Unable to replay the backtest: " + e.getMessage());
			return;
		}
		stringConsumer.accept("Backtest: " + base.describe());
		double positionSize = positionSizePercent(base);
		stringConsumer.accept(format("Random position size: %.2f %% of account value", positionSize));

		Calibration calibration = findProbability(base.trades(), lengths, positionSize);
		if (isStopped.get() || calibration == null) {
			return;
		}
		stringConsumer.accept(format("Found trade probability for RandomStrategy: %.5f", calibration.probability()));
		stringConsumer.accept("Random trading: " + calibration.profile().describe());
		testRandom(baseAccount, calibration.probability(), lengths, positionSize);
	}

	public void stop() {
		isStopped.set(true);
	}

	private static int[] tradeLengths(Account account) {
		return account.getAllHistoryRecords().stream()
				.filter(record -> !record.open())
				.mapToInt(PositionHistoryRecord::tradeLength)
				.toArray();
	}

	/**
	 * Replays the backtest's actions to measure its exposure and number of open positions at every session.
	 */
	private Profile profileOf(Account baseAccount, int[] lengths) throws AccountActionException {
		ExposureMeter meter = new ExposureMeter();
		Account replayed = new Account(0);
		AccountReplayer.replayInto(replayed, marketData, baseAccount.getAccountHistory().getActions(), (time, netDeposits) -> {
			if (!time.isAfter(endTime)) {
				meter.add(replayed);
			}
		});
		return meter.profile(lengths.length, average(lengths));
	}

	/**
	 * Same holding lengths and number of trades give the same average number of open positions, so this size gives
	 * the same average exposure.
	 */
	private static double positionSizePercent(Profile base) {
		if (base.openPositions() <= 0) {
			return 0;
		}
		return Math.min(MAX_POSITION_SIZE_PERCENT, base.exposurePercent() / base.openPositions());
	}

	/**
	 * Searches the entry probability until random trading makes about as many trades as the backtest. Stops after a
	 * fixed number of steps and then uses the closest result.
	 */
	private Calibration findProbability(int requiredTrades, int[] lengths, double positionSize) {
		ProbabilitySearch search = new ProbabilitySearch(requiredTrades);
		Calibration best = null;
		for (int step = 0; step < MAX_CALIBRATION_STEPS && !isStopped.get() && search.canContinue(); step++) {
			stringConsumer.accept(format("Trying trade probability %.5f", search.probability()));
			Calibration current = tryProbability(search.probability(), lengths, positionSize);
			stringConsumer.accept("Got average trades: " + current.profile().trades());
			if (search.isCloser(current, best)) {
				best = current;
			}
			if (search.isCloseEnough(current.profile().trades())) {
				return current;
			}
			search.next(current.profile().trades());
		}
		if (isStopped.get()) {
			return null;
		}
		stringConsumer.accept("Could not match the number of trades exactly, using the closest result.");
		return best;
	}

	/**
	 * The number of trades grows with the probability, but not proportionally (cash runs out), so every step either
	 * scales the probability or halves the known bracket.
	 */
	private static final class ProbabilitySearch {
		private final int requiredTrades;
		private double low;
		private double high = MAX_PROBABILITY;
		private double probability = INITIAL_PROBABILITY;
		private boolean exhausted;

		ProbabilitySearch(int requiredTrades) {
			this.requiredTrades = requiredTrades;
		}

		double probability() {
			return probability;
		}

		boolean canContinue() {
			return !exhausted;
		}

		boolean isCloseEnough(int trades) {
			return Math.abs(trades - requiredTrades) <= requiredTrades / 20;
		}

		boolean isCloser(Calibration current, Calibration best) {
			return best == null || distance(current) < distance(best);
		}

		private int distance(Calibration calibration) {
			return Math.abs(calibration.profile().trades() - requiredTrades);
		}

		void next(int trades) {
			if (trades < requiredTrades) {
				// Even the highest probability gives too few trades
				exhausted = probability >= MAX_PROBABILITY;
				low = probability;
			} else {
				high = probability;
			}
			double scaled = trades == 0 ? probability * 10 : probability * requiredTrades / trades;
			probability = scaled > low && scaled < high ? scaled : (low + high) / 2;
		}
	}

	private Calibration tryProbability(double probability, int[] lengths, double positionSize) {
		List<Profile> profiles = new ArrayList<>();
		try (ExecutorService threadExecutor = Executors.newWorkStealingPool()) {
			List<Future<Optional<Profile>>> futures = new ArrayList<>();
			for (int i = 0; i < CALIBRATION_RUNS; i++) {
				futures.add(threadExecutor.submit(() -> tryExecute(probability, lengths, positionSize).map(Run::profile)));
			}
			for (var future : futures) {
				future.get().ifPresent(profiles::add);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			stringConsumer.accept("Error: " + e.getCause());
		}
		return new Calibration(probability, Profile.average(profiles));
	}

	private void testRandom(Account baseAccount, double probability, int[] lengths, double positionSize) {
		stringConsumer.accept("Starting " + iterations + " iterations...");
		AtomicInteger allCount = new AtomicInteger();
		AtomicInteger betterCount = new AtomicInteger();
		try (ExecutorService executor = Executors.newWorkStealingPool()) {
			for (int i = 0; i < iterations; i++) {
				executor.submit(() -> tryExecute(probability, lengths, positionSize).ifPresent(run -> {
					if (run.account().getNAV() > baseAccount.getNAV()) {
						betterCount.incrementAndGet();
					}
					int doneCounter = allCount.incrementAndGet();
					if (doneCounter % 100 == 0) {
						stringConsumer.accept("Done iterations " + doneCounter);
					}
				}));
			}
		}
		int done = allCount.get();
		if (done == 0) {
			return;
		}
		stringConsumer.accept(format("RandomStrategy achieved better result %d times out of %d (p ≈ %.3f).",
				betterCount.get(), done, betterCount.get() / (double) done));
	}

	private Optional<Run> tryExecute(double probability, int[] lengths, double positionSize) {
		if (isStopped.get()) {
			return empty();
		}
		try {
			return of(execute(probability, lengths, positionSize));
		} catch (AccountActionException e) {
			stringConsumer.accept("Error: Unable to execute account action: " + e.getMessage());
			return empty();
		}
	}

	private Run execute(double probability, int[] lengths, double positionSize) throws AccountActionException {
		RandomStrategy randomStrategy = new RandomStrategy();
		randomStrategy.setTradeProbability(probability);
		randomStrategy.setHoldingLengths(lengths);
		randomStrategy.setPositionSizePercent(positionSize);
		ExposureMeter meter = new ExposureMeter();
		StrategyExecutor strategyExecutor = new StrategyExecutor().withTimeRange(startTime, endTime)
																  .withInitialCash(initialCash)
																  .withCommissionRate(commissionRate)
																  .withBarExecutedConsumer((time, account, orders) -> meter.add(account));
		Account account = strategyExecutor.execute(marketData, randomStrategy);
		int[] randomLengths = tradeLengths(account);
		return new Run(account, meter.profile(randomLengths.length, average(randomLengths)));
	}

	private static double average(int[] values) {
		if (values.length == 0) {
			return 0;
		}
		long sum = 0;
		for (int value : values) {
			sum += value;
		}
		return sum / (double) values.length;
	}

	/**
	 * Averages over sessions: exposure (positions at the session's prices as a share of the account value) and the
	 * number of open positions.
	 */
	private static final class ExposureMeter {
		private double exposureSum;
		private double openPositionsSum;
		private int sessions;

		void add(Account account) {
			double nav = account.getNAV();
			if (nav > 0) {
				exposureSum += (nav - account.getCash()) / nav * 100;
			}
			openPositionsSum += account.getPositions().size();
			sessions++;
		}

		Profile profile(int trades, double averageLength) {
			if (sessions == 0) {
				return new Profile(trades, averageLength, 0, 0);
			}
			return new Profile(trades, averageLength, exposureSum / sessions, openPositionsSum / sessions);
		}
	}

	private record Profile(int trades, double averageLength, double exposurePercent, double openPositions) {

		static Profile average(List<Profile> profiles) {
			if (profiles.isEmpty()) {
				return new Profile(0, 0, 0, 0);
			}
			return new Profile(
					(int) Math.round(profiles.stream().mapToInt(Profile::trades).average().orElse(0)),
					profiles.stream().mapToDouble(Profile::averageLength).average().orElse(0),
					profiles.stream().mapToDouble(Profile::exposurePercent).average().orElse(0),
					profiles.stream().mapToDouble(Profile::openPositions).average().orElse(0));
		}

		String describe() {
			return format("%d trades, average length %.1f sessions, exposure %.0f %%, open positions %.1f",
					trades, averageLength, exposurePercent, openPositions);
		}
	}

	private record Calibration(double probability, Profile profile) {
	}

	private record Run(Account account, Profile profile) {
	}
}
