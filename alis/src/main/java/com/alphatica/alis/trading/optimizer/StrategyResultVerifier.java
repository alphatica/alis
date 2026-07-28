package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.scorer.AccountScorer;
import com.alphatica.alis.trading.account.scorer.ScoredAccount;
import com.alphatica.alis.trading.strategy.Strategy;
import com.alphatica.alis.trading.strategy.StrategyExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

final class StrategyResultVerifier {

	private static final int VERIFICATION_RUNS = 49;

	private final Supplier<Strategy> strategyFactory;
	private final MarketData marketData;
	private final Supplier<StrategyExecutor> executorFactory;
	private final Supplier<AccountScorer> scorerFactory;
	private final Consumer<Exception> exceptionHandler;
	private final ReducedMarketDataFactory reducedMarketDataFactory = new ReducedMarketDataFactory();

	StrategyResultVerifier(Supplier<Strategy> strategyFactory, MarketData marketData,
						   Supplier<StrategyExecutor> executorFactory, Supplier<AccountScorer> scorerFactory,
						   Consumer<Exception> exceptionHandler) {
		this.strategyFactory = strategyFactory;
		this.marketData = marketData;
		this.executorFactory = executorFactory;
		this.scorerFactory = scorerFactory;
		this.exceptionHandler = exceptionHandler;
	}

	Optional<ScoredAccount> verify(ResultVerifier verifier, Map<String, Object> parameters) throws IllegalAccessException {
		return switch (verifier) {
			case NONE -> execute(parameters, marketData, ignored -> { });
			case REMOVE_MARKETS -> verifyReducedMarkets(parameters);
			case REMOVE_ORDERS -> verifyReducedOrders(parameters);
			case FUZZY_START_TIME -> verifyFuzzyStartTime(parameters);
		};
	}

	private Optional<ScoredAccount> verifyReducedMarkets(Map<String, Object> parameters) throws IllegalAccessException {
		List<ScoredAccount> scores = new ArrayList<>();
		while (scores.size() < VERIFICATION_RUNS) {
			execute(parameters, reducedMarketDataFactory.create(marketData), ignored -> { }).ifPresent(scores::add);
		}
		return median(scores);
	}

	private Optional<ScoredAccount> verifyReducedOrders(Map<String, Object> parameters) throws IllegalAccessException {
		List<ScoredAccount> scores = new ArrayList<>();
		while (scores.size() < VERIFICATION_RUNS) {
			execute(parameters, marketData, executor -> executor.skipTrades(0.5)).ifPresent(scores::add);
		}
		return median(scores);
	}

	private Optional<ScoredAccount> verifyFuzzyStartTime(Map<String, Object> parameters) throws IllegalAccessException {
		Time preferredStartTime = executorFactory.get().getTimeFrom();
		List<Time> startTimes = FuzzyStartTimeSelector.select(marketData.getTimes(), preferredStartTime, VERIFICATION_RUNS);
		List<ScoredAccount> scores = new ArrayList<>();
		for (Time startTime : startTimes) {
			execute(parameters, marketData, executor -> executor.withTimeFrom(startTime)).ifPresent(scores::add);
		}
		return median(scores);
	}

	private Optional<ScoredAccount> execute(Map<String, Object> parameters, MarketData executionData,
										 Consumer<StrategyExecutor> configureExecutor) throws IllegalAccessException {
		Strategy strategy = strategyFactory.get();
		Optimizer.copyParameters(parameters, strategy);
		AccountScorer scorer = scorerFactory.get();
		StrategyExecutor executor = executorFactory.get();
		configureExecutor.accept(executor);
		try {
			var account = executor.execute(executionData, strategy);
			double score = scorer.score(account, strategy.getCustomStats());
			return Optional.of(new ScoredAccount(score, account));
		} catch (AccountActionException exception) {
			exceptionHandler.accept(exception);
			return Optional.empty();
		}
	}

	private static Optional<ScoredAccount> median(List<ScoredAccount> scores) {
		if (scores.isEmpty()) {
			return Optional.empty();
		}
		scores.sort(null);
		return Optional.of(scores.get(scores.size() / 2));
	}
}
