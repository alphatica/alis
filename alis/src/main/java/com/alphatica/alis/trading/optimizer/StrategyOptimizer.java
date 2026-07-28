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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class StrategyOptimizer extends Optimizer {

	private final AtomicInteger counter = new AtomicInteger(0);
	private final AtomicBoolean isStopped = new AtomicBoolean(false);
	private final long maxCounter;
	private final ResultVerifier resultVerifier;
	private final StrategyResultVerifier verifier;
	private final ParamsSelector paramsSelector;
	private final AtomicInteger iterationsStarted = new AtomicInteger(0);
	private final AtomicLong sumMillisElapsed = new AtomicLong(0);

	private BiConsumer<OptimizerScore, Account> scoreCallback;
	private Consumer<Exception> exceptionCallback;

	public StrategyOptimizer(Supplier<Strategy> strategyFactory, MarketData marketData, Supplier<StrategyExecutor> executorFactory, Supplier<AccountScorer> scorerFactory, ResultVerifier resultVerifier, ParametersSelection parametersSelection, long maxCounter) throws OptimizerException {
		this.maxCounter = maxCounter;
		this.resultVerifier = resultVerifier;
		this.verifier = new StrategyResultVerifier(strategyFactory, marketData, executorFactory, scorerFactory, this::passException);
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
					optimizeOnce();
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

	private void optimizeOnce() throws IllegalAccessException {
		Map<String, Object> nextParams = paramsSelector.next();
		if (nextParams.isEmpty()) {
			return;
		}
		verifier.verify(resultVerifier, nextParams)
				.ifPresent(result -> registerScore(result.score(), result.account(), nextParams));
	}

	private synchronized void registerScore(double score, Account account, Map<String, Object> parameters) {
		OptimizerScore newScore = new OptimizerScore(score, parameters);
		paramsSelector.registerScore(newScore);
		if (scoreCallback != null) {
			scoreCallback.accept(newScore, account);
		}
	}

	public void stop() {
		isStopped.set(true);
	}

	public int getLoopCount() {
		return counter.get();
	}

}
