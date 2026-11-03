package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.scorer.AccountScorer;
import com.alphatica.alis.trading.strategy.Strategy;
import com.alphatica.alis.trading.strategy.StrategyExecutor;
import com.alphatica.alis.trading.optimizer.paramsselector.ParamsSelector;
import com.alphatica.alis.trading.optimizer.params.Validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class StrategyOptimizer extends Optimizer {

	private final AtomicInteger counter = new AtomicInteger(0);
	private final AtomicBoolean isStopped = new AtomicBoolean(false);
	private final AtomicReference<Exception> failure = new AtomicReference<>();
	private final long maxCounter;
	private final ResultVerifier resultVerifier;
	private final StrategyResultVerifier verifier;
	private final ParamsSelector paramsSelector;
	private final AtomicInteger iterationsStarted = new AtomicInteger(0);

	public StrategyOptimizer(Supplier<Strategy> strategyFactory, MarketData marketData, Supplier<StrategyExecutor> executorFactory, Supplier<AccountScorer> scorerFactory, ResultVerifier resultVerifier, ParametersSelection parametersSelection, long maxCounter) throws OptimizerException {
		this.maxCounter = maxCounter;
		this.resultVerifier = resultVerifier;
		this.verifier = new StrategyResultVerifier(strategyFactory, marketData, executorFactory, scorerFactory, this::fail);
		var fields = strategyFactory.get().getClass().getDeclaredFields();
		Validator.validate(fields);
		ParamsStepsSet paramsStepsSet = buildParamsStepsSet(fields);
		this.paramsSelector = ParamsSelector.get(parametersSelection, paramsStepsSet);
	}

	/**
	 * Runs on all processors until {@code maxCounter} iterations are done or {@link #stop()} is called.
	 * The first error stops the optimization and is thrown.
	 *
	 * @param onScore called for every scored set of parameters, one call at a time
	 */
	public void optimize(BiConsumer<OptimizerScore, Account> onScore) throws OptimizerException {
		optimize(onScore, List.of());
	}

	public void optimize(BiConsumer<OptimizerScore, Account> onScore, List<Map<String, Object>> seeds) throws OptimizerException {
		for (Map<String, Object> seed : seeds) {
			if (iterationsStarted.incrementAndGet() > maxCounter || isStopped.get()) {
				break;
			}
			runIteration(() -> score(seed, false, onScore));
		}
		int processors = Runtime.getRuntime().availableProcessors();
		List<Thread> threads = new ArrayList<>();
		for (int i = 0; i < processors; i++) {
			threads.add(startWork(onScore));
		}
		waitForThreads(threads);
		Exception exception = failure.get();
		if (exception != null) {
			throw new OptimizerException("Optimization failed: " + exception, exception);
		}
	}

	private Thread startWork(BiConsumer<OptimizerScore, Account> onScore) {
		Runnable task = () -> {
			while (iterationsStarted.incrementAndGet() <= maxCounter && !isStopped.get()) {
				runIteration(() -> optimizeOnce(onScore));
			}
		};
		Thread thread = new Thread(task);
		thread.start();
		return thread;
	}

	private interface Iteration {
		void run() throws IllegalAccessException;
	}

	private void runIteration(Iteration iteration) {
		try {
			iteration.run();
			counter.incrementAndGet();
		} catch (IllegalAccessException | RuntimeException e) {
			fail(e);
		}
	}

	private void fail(Exception e) {
		failure.compareAndSet(null, e);
		stop();
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

	private void optimizeOnce(BiConsumer<OptimizerScore, Account> onScore) throws IllegalAccessException {
		Map<String, Object> nextParams = paramsSelector.next();
		if (nextParams.isEmpty()) {
			return;
		}
		score(nextParams, true, onScore);
	}

	private void score(Map<String, Object> params, boolean toSelector, BiConsumer<OptimizerScore, Account> onScore)
			throws IllegalAccessException {
		verifier.verify(resultVerifier, params)
				.ifPresent(result -> registerScore(result.score(), result.account(), params, toSelector, onScore));
	}

	private synchronized void registerScore(double score, Account account, Map<String, Object> parameters,
											boolean toSelector, BiConsumer<OptimizerScore, Account> onScore) {
		OptimizerScore newScore = new OptimizerScore(score, parameters);
		if (toSelector) {
			paramsSelector.registerScore(newScore);
		}
		onScore.accept(newScore, account);
	}

	public void stop() {
		isStopped.set(true);
	}

	public int getLoopCount() {
		return counter.get();
	}

}
