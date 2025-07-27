package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.data.StandardMarketData;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.TradeStats;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.strategy.Strategy;
import com.alphatica.alis.trading.strategy.StrategyExecutor;
import com.alphatica.alis.trading.optimizer.paramsselector.ParamsSelector;
import com.alphatica.alis.trading.account.scorer.AccountScorer;
import com.alphatica.alis.trading.account.scorer.ScoredAccount;
import com.alphatica.alis.trading.optimizer.params.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class StrategyOptimizer extends Optimizer {

	private final Supplier<Strategy> strategyFactory;
	private final MarketData marketData;
	private final Supplier<StrategyExecutor> executorFactory;
	private final AtomicInteger counter = new AtomicInteger(0);
	private final AtomicBoolean isStopped = new AtomicBoolean(false);
	private final Supplier<AccountScorer> scorerFactory;
	private final long maxCounter;
	private final ResultVerifier resultVerifier;
	private final ParamsSelector paramsSelector;
	private final AtomicInteger iterationsStarted = new AtomicInteger(0);
	private final AtomicLong sumMillisElapsed = new AtomicLong(0);

	private BiConsumer<OptimizerScore, Account> scoreCallback;
	private Consumer<Exception> exceptionCallback;

	public StrategyOptimizer(Supplier<Strategy> strategyFactory, MarketData marketData, Supplier<StrategyExecutor> executorFactory, Supplier<AccountScorer> scorerFactory, ResultVerifier resultVerifier, ParametersSelection parametersSelection, long maxCounter) throws OptimizerException {
		this.strategyFactory = strategyFactory;
		this.marketData = marketData;
		this.executorFactory = executorFactory;
		this.scorerFactory = scorerFactory;
		this.maxCounter = maxCounter;
		this.resultVerifier = resultVerifier;
		var fields = strategyFactory.get().getClass().getDeclaredFields();
		Validator.validate(fields);
		ParamsStepsSet paramsStepsSet = buildParamsStepsSet(fields);
		this.paramsSelector = ParamsSelector.get(parametersSelection, paramsStepsSet);
	}

	public StrategyOptimizer setExceptionCallback(Consumer<Exception> callback) {
		this.exceptionCallback = callback;
		return this;
	}

	public void startOptimizations() {
		int processors = Runtime.getRuntime().availableProcessors();
		List<Thread> threads = new ArrayList<>();
		long start = System.nanoTime();
		for(int i = 0; i < processors; i++) {
			Thread thread = startWork();
			threads.add(thread);
		}
		waitForThreads(threads);
		System.out.println("Elapsed: " + (System.nanoTime() - start) / 1_000_000_000);
	}

	private Thread startWork() {
		Runnable task = () -> {
			while(iterationsStarted.incrementAndGet() <= maxCounter && !isStopped.get()) {
				try {
					long startTime = System.nanoTime();
					switch (resultVerifier) {
						case NONE -> optimizeWithAllTradesAndMarkets();
						case REMOVE_MARKETS -> optimizeWithReducedMarkets();
						case REMOVE_ORDERS -> optimizeWithReducedOrders();
					}
					long endTime = System.nanoTime();
					counter.incrementAndGet();
					updateAverageTime(startTime, endTime);
				} catch (IllegalAccessException e) {
					passException(e);
				}
			}
		};
		Thread thread = new Thread(task);
		thread.start();
		return thread;
	}

	private void updateAverageTime(long startTime, long endTime) {
		long millis = (endTime - startTime) / 1_000_000;
		var millisElapsed = sumMillisElapsed.addAndGet(millis);
		double average = (double)millisElapsed / counter.get();
		System.out.printf("Average time per optimization loop: %.1f ms%n", average);
	}

	private void passException(Exception e) {
		if (exceptionCallback != null) {
			exceptionCallback.accept(e);
		}
	}

	public void registerScoreCallback(BiConsumer<OptimizerScore, Account> scoreCallback) {
		this.scoreCallback = scoreCallback;
	}

	private static void waitForThreads(List<Thread> handlers) {
		while (!handlers.isEmpty()) {
			try {
				handlers.getLast().join();
				handlers.removeLast();
			} catch (Exception e) {
				/* try again */
			}
		}
	}

	private void optimizeWithAllTradesAndMarkets() throws IllegalAccessException {
		Map<String, Object> nextParams = paramsSelector.next();
		if (nextParams.isEmpty()) {
			return;
		}
		Strategy strategy = strategyFactory.get();
		copyParameters(nextParams, strategy);
		AccountScorer scorer = scorerFactory.get();
		StrategyExecutor executor = executorFactory.get();
		try {
			Account account = executor.execute(marketData, strategy);
			double score = scorer.score(account, strategy.getCustomStats());
			registerScore(score, account, nextParams);
		} catch (AccountActionException e) {
			passException(e);
		}
	}

	private void optimizeWithReducedMarkets() throws IllegalAccessException {
		final int maxOptimizations = 49;
		Map<String, Object> params = paramsSelector.next();
		if (params.isEmpty()) {
			return;
		}
		List<ScoredAccount> scoredAccounts = new ArrayList<>();
		while (scoredAccounts.size() < maxOptimizations) {
			AccountScorer scorer = scorerFactory.get();
			StrategyExecutor executor = executorFactory.get();
			Strategy strategy = strategyFactory.get();
			copyParameters(params, strategy);
			StandardMarketData newMarketData = new StandardMarketData();
			try {
				Map<MarketName, Market> map = marketData.listMarkets(acceptMarket()).stream().collect(Collectors.toMap(Market::getName, m -> m));
				newMarketData.addMarkets(map);
				Account account = executor.execute(newMarketData, strategy);
				double score = scorer.score(account, strategy.getCustomStats());
				scoredAccounts.add(new ScoredAccount(score, account));
			} catch (AccountActionException e) {
				passException(e);
			}
		}
		Collections.sort(scoredAccounts);
		ScoredAccount medianScore = scoredAccounts.get(scoredAccounts.size() / 2);
		registerScore(medianScore.score(), medianScore.account(), params);
	}

	private static Predicate<Market> acceptMarket() {
		return m -> m.getType() != MarketType.STOCK || ThreadLocalRandom.current().nextDouble() > 0.5;
	}

	private void optimizeWithReducedOrders() throws IllegalAccessException {
		final int maxOptimizations = 49;
		Map<String, Object> params = paramsSelector.next();
		if (params.isEmpty()) {
			return;
		}
		List<ScoredAccount> scoredAccounts = new ArrayList<>();
		while (scoredAccounts.size() < maxOptimizations) {
			AccountScorer scorer = scorerFactory.get();
			StrategyExecutor executor = executorFactory.get();
			Strategy strategy = strategyFactory.get();
			copyParameters(params, strategy);
			try {
				executor.skipTrades(0.5);
				Account account = executor.execute(marketData, strategy);
				double score = scorer.score(account, strategy.getCustomStats());
				scoredAccounts.add(new ScoredAccount(score, account));
			} catch (AccountActionException e) {
				passException(e);
			}
		}
		Collections.sort(scoredAccounts);
		ScoredAccount medianScore = scoredAccounts.get(scoredAccounts.size() / 2);
		registerScore(medianScore.score(), medianScore.account(), params);
	}

	private synchronized void registerScore(double score, Account account, Map<String, Object> parameters) {
		OptimizerScore newScore = new OptimizerScore(score, parameters);
		paramsSelector.registerScore(newScore);
		if (scoreCallback != null) {
			scoreCallback.accept(newScore, account);
		}
//		testOOs(account, parameters);
	}

	private void testOOs(Account inSampleAccount, Map<String, Object> parameters) {
		Executor threadExecutor = Executors.newVirtualThreadPerTaskExecutor();
		threadExecutor.execute(() -> {
			StrategyExecutor executor = new StrategyExecutor().withTimeRange(new Time(20200101), new Time(20260101));
			Strategy strategy = strategyFactory.get();
			try {
				copyParameters(parameters, strategy);
				Account outOfSampleAccount = executor.execute(marketData, strategy);
				TradeStats inSampleStats = inSampleAccount.getAccountHistory().getStats();
				System.out.printf("%.0f,%.2f,%.2f,%.2f,%.2f,%.2f,%.0f,%.2f%n",
						inSampleAccount.getNAV(),
						inSampleAccount.getMaxDD(),
						inSampleStats.expectancy(),
						inSampleStats.profitFactor(),
						inSampleStats.profitPerTrade(),
						inSampleStats.accuracy(),
						outOfSampleAccount.getNAV(),
						outOfSampleAccount.getMaxDD()
				);
			} catch (IllegalAccessException | AccountActionException e) {
				throw new RuntimeException(e);
			}
		});
	}

	public void stop() {
		isStopped.set(true);
	}

	public int getLoopCount() {
		return counter.get();
	}

}
