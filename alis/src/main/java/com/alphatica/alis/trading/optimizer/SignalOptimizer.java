package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.trading.optimizer.params.Validator;
import com.alphatica.alis.trading.optimizer.paramsselector.ParamsSelector;
import com.alphatica.alis.trading.signalcheck.SignalExecutor;
import com.alphatica.alis.trading.signalcheck.scoregenerator.ScoreGenerator;
import com.alphatica.alis.trading.signalcheck.tradesignal.TradeSignal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class SignalOptimizer extends Optimizer {

	private final Supplier<TradeSignal> tradeSignalSupplier;
	private final MarketData marketData;
	private final Time startTime;
	private final Time endTime;
	private final Predicate<TimeMarketData> marketFilter;
	private final float commissionRate;
	private final boolean tradeSecondarySignals;
	private final Supplier<ScoreGenerator> scoreGeneratorSupplier;
	private final ParamsSelector paramsSelector;
	private final AtomicBoolean isStopped = new AtomicBoolean(false);

	private volatile double bestScore = 0;

	public SignalOptimizer(Supplier<TradeSignal> tradeSignalSupplier, MarketData marketData, Time startTime, Time endTime,
						   Predicate<TimeMarketData> marketFilter, float commissionRate, boolean tradeSecondarySignals,
						   ParametersSelection parametersSelection, Supplier<ScoreGenerator> scoreGeneratorSupplier) throws OptimizerException {
		this.tradeSignalSupplier = tradeSignalSupplier;
		this.marketData = marketData;
		this.startTime = startTime;
		this.endTime = endTime;
		this.marketFilter = marketFilter;
		this.commissionRate = commissionRate;
		this.tradeSecondarySignals = tradeSecondarySignals;
		this.scoreGeneratorSupplier = scoreGeneratorSupplier;
		var fields = tradeSignalSupplier.get().getClass().getDeclaredFields();
		Validator.validate(fields);
		ParamsStepsSet paramsStepsSet = buildParamsStepsSet(fields);
		this.paramsSelector = ParamsSelector.get(parametersSelection, paramsStepsSet);
	}

	public void run() {
		int processors = Runtime.getRuntime().availableProcessors();
		List<Thread> threads = new ArrayList<>();
		for(int i = 0; i < processors; i++) {
			Thread thread = startWork();
			threads.add(thread);
		}
		waitForThreads(threads);
	}

	private Thread startWork() {
		Runnable runnable = () -> {
			while(!isStopped.get()) {
				optimizeOnce();
			}
		};
		Thread t = new Thread(runnable);
		t.start();
		return t;
	}

	private void optimizeOnce() {
		Map<String, Object> nextParams = paramsSelector.next();
		if (nextParams.isEmpty()) {
			isStopped.set(true);
			return;
		}
		Supplier<TradeSignal> optimizedTradeSignalSupplier = () -> {
			var tradeSignal = tradeSignalSupplier.get();
			try {
				copyParameters(nextParams, tradeSignal);
				return tradeSignal;
			} catch (IllegalAccessException e) {
				System.out.println(e);
				return null;
			}
		};
		var signalExecutor = new SignalExecutor(optimizedTradeSignalSupplier, startTime, endTime, marketData, marketFilter, commissionRate, tradeSecondarySignals, scoreGeneratorSupplier.get())
				.useCachedMarketData();
		var score = signalExecutor.execute();
		var optimizerScore = new OptimizerScore(score, nextParams);
		paramsSelector.registerScore(optimizerScore);
		show(optimizerScore);
	}

	private void show(OptimizerScore optimizerScore) {
		var code = optimizerScore.formatParamsAsJavaCode();
		synchronized (this) {
			if (optimizerScore.score() <= bestScore) {
				return;
			}
			bestScore = optimizerScore.score();
			System.out.println("______________________________________________________________________");
			for(String line: code) {
				System.out.println(line);
			}
			System.out.println("Score: " + optimizerScore.score());
		}
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
}
