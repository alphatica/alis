package com.alphatica.alis.studio.view.window.trading.strategies.backtest;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.strategy.RandomStrategy;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.TradeStats;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.strategy.StrategyExecutor;

import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;

public class RandomStrategyComparator {

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
		TradeStats stats = baseAccount.getAccountHistory().getStats();
		int requiredTrades = stats.trades();
		stringConsumer.accept("Required trades " + requiredTrades);

		double probability = findProbability(requiredTrades);
		stringConsumer.accept(format("Found trade probability for RandomStrategy: %.5f",probability));
		testRandom(baseAccount, probability);
	}

	public void stop() {
		isStopped.set(true);
	}

	private void testRandom(Account baseAccount, double probability) {
		stringConsumer.accept("Starting " + iterations + " iterations...");
		AtomicInteger allCount = new AtomicInteger();
		AtomicInteger betterCount = new AtomicInteger();
		try (ExecutorService executor = Executors.newWorkStealingPool()) {
			for (int i = 0; i < iterations; i++) {
				executor.submit(() -> tryExecuteWithProbability(probability).ifPresent(testAccount -> {
					if (testAccount.getNAV() > baseAccount.getNAV()) {
						betterCount.incrementAndGet();
					}
					int doneCounter = allCount.incrementAndGet();
					if (doneCounter % 100 == 0) {
						stringConsumer.accept("Done iterations " + doneCounter);
					}
				}));
			}
		}
		stringConsumer.accept("RandomStrategy achieved better result " + betterCount.get() + " times.");
	}

	private Optional<Account> tryExecuteWithProbability(double probability) {
		try {
			return executeWithProbability(probability);
		} catch (AccountActionException e) {
			stringConsumer.accept("Error: Unable to execute account action: " + e.getMessage());
			return empty();
		}
	}

	private Optional<Account> executeWithProbability(double probability) throws AccountActionException {
		if (isStopped.get()) {
			return empty();
		}
		RandomStrategy randomStrategy = new RandomStrategy();
		randomStrategy.setTradeProbability(probability);
		StrategyExecutor strategyExecutor = new StrategyExecutor().withTimeRange(startTime, endTime)
																  .withInitialCash(initialCash)
																  .withCommissionRate(commissionRate);

		Account account = strategyExecutor.execute(marketData, randomStrategy);
		return of(account);
	}

	private double findProbability(int requiredTrades) {
		double probability = 0.1;
		int averageTrades = 0;
		while (!closeEnough(averageTrades, requiredTrades)) {
			stringConsumer.accept(format("Trying trade probability %.5f", probability));
			averageTrades = tryProbability(probability);
			stringConsumer.accept("Got average trades: " + averageTrades);
			probability *= requiredTrades / (double) averageTrades;
		}
		return probability;
	}

	private boolean closeEnough(int averageTrades, int requiredTrades) {
		return Math.abs(averageTrades - requiredTrades) <= requiredTrades / 20;
	}

	private int tryProbability(double probability) {
		final int attempts = 10;
		AtomicInteger sumTrades = new AtomicInteger();
		try(ExecutorService threadExecutor = Executors.newWorkStealingPool()) {
			for (int i = 0; i < attempts; i++) {
				threadExecutor.execute(() -> tryExecuteWithProbability(probability).ifPresent(testAccount ->
						sumTrades.addAndGet(testAccount.getAccountHistory().getStats().trades())));
			}
		}
		return (int)Math.round(sumTrades.get() / (double)attempts);
	}
}
