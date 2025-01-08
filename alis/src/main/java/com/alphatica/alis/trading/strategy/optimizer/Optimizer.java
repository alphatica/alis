package com.alphatica.alis.trading.strategy.optimizer;

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
import com.alphatica.alis.trading.strategy.optimizer.paramsselector.ParamsSelector;
import com.alphatica.alis.trading.strategy.optimizer.paramsselector.RandomParamsSelector;
import com.alphatica.alis.trading.account.scorer.AccountScorer;
import com.alphatica.alis.trading.account.scorer.ScoredAccount;
import com.alphatica.alis.trading.strategy.params.BoolParam;
import com.alphatica.alis.trading.strategy.params.DoubleParam;
import com.alphatica.alis.trading.strategy.params.IntParam;
import com.alphatica.alis.trading.strategy.params.Validator;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Optimizer {

	private final Supplier<Strategy> strategyFactory;
	private final MarketData marketData;
	private final Supplier<StrategyExecutor> executorFactory;
	private final AtomicInteger counter = new AtomicInteger(0);
	private final AtomicBoolean isStopped = new AtomicBoolean(false);
	private final Supplier<AccountScorer> scorerFactory;
	private final long maxCounter;
	private final ResultVerifier resultVerifier;
	private final ParamsSelector paramsSelector;
	private final RandomParamsSelector randomParamsSelector;
	private BiConsumer<OptimizerScore, Account> scoreCallback;
	private Consumer<Exception> exceptionCallback;
	private final AtomicInteger iterationsStarted = new AtomicInteger(0);

	public Optimizer(Supplier<Strategy> strategyFactory, MarketData marketData, Supplier<StrategyExecutor> executorFactory, Supplier<AccountScorer> scorerFactory, ResultVerifier resultVerifier, ParametersSelection parametersSelection, long maxCounter) throws OptimizerException {
		this.strategyFactory = strategyFactory;
		this.marketData = marketData;
		this.executorFactory = executorFactory;
		this.scorerFactory = scorerFactory;
		this.maxCounter = maxCounter;
		this.resultVerifier = resultVerifier;
		Strategy s = strategyFactory.get();
		Validator.validate(s);
		ParamsStepsSet paramsStepsSet = buildParamsStepsSet(s);
		this.randomParamsSelector = new RandomParamsSelector(paramsStepsSet);
		this.paramsSelector = ParamsSelector.get(parametersSelection, paramsStepsSet);
	}

	public static long computeAllPermutations(Strategy strategy) {
		return buildParamsStepsSet(strategy).computePermutations();
	}

	public Optimizer setExceptionCallback(Consumer<Exception> callback) {
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
					switch (resultVerifier) {
						case NONE -> optimizeWithAllTradesAndMarkets();
						case REMOVE_MARKETS -> optimizeWithReducedMarkets();
						case REMOVE_ORDERS -> optimizeWithReducedOrders();
					}
					counter.incrementAndGet();
				} catch (IllegalAccessException e) {
					passException(e);
				}
			}
		};
		Thread thread = new Thread(task);
		thread.start();
		return thread;
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
		Map<String, Object> nextParams = getNextParams();
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
		Map<String, Object> params = getNextParams();
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

	private Map<String, Object> getNextParams() {
		Map<String, Object> params = paramsSelector.next();
		if (params.isEmpty()) {
			params = randomParamsSelector.next();
		}
		return params;
	}

	private static Predicate<Market> acceptMarket() {
		return m -> m.getType() != MarketType.STOCK || ThreadLocalRandom.current().nextDouble() > 0.5;
	}

	private void optimizeWithReducedOrders() throws IllegalAccessException {
		final int maxOptimizations = 49;
		Map<String, Object> params = getNextParams();
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

	@SuppressWarnings("java:S3011")
	private void copyParameters(Map<String, Object> params, Strategy strategy) throws IllegalAccessException {
		Field[] fields = strategy.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (isParameterField(field)) {
				field.setAccessible(true);
				field.set(strategy, params.get(field.getName()));
			}
		}
		strategy.paramsChanged();
	}

	private static ParamsStepsSet buildParamsStepsSet(Strategy s) {
		ParamsStepsSet paramsStepsSet = new ParamsStepsSet();
		Field[] fields = s.getClass().getDeclaredFields();
		for (Field field : fields) {
			if (field.isAnnotationPresent(BoolParam.class)) {
				paramsStepsSet.addParamSteps(field.getName(), new ParamSteps());
			}
			if (field.isAnnotationPresent(IntParam.class)) {
				IntParam p = field.getAnnotation(IntParam.class);
				paramsStepsSet.addParamSteps(field.getName(), new ParamSteps(p.start(), p.step(), p.end()));
			}
			if (field.isAnnotationPresent(DoubleParam.class)) {
				DoubleParam p = field.getAnnotation(DoubleParam.class);
				paramsStepsSet.addParamSteps(field.getName(), new ParamSteps(p.start(), p.step(), p.end()));
			}
		}
		return paramsStepsSet;
	}

	public static boolean isParameterField(Field field) {
		return field.isAnnotationPresent(BoolParam.class) || field.isAnnotationPresent(IntParam.class) || field.isAnnotationPresent(DoubleParam.class);
	}
}
