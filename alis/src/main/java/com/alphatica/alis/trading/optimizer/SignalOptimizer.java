package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.trading.optimizer.params.Validator;
import com.alphatica.alis.trading.optimizer.paramsselector.ParamsSelector;
import com.alphatica.alis.trading.signalcheck.AllocationPolicy;
import com.alphatica.alis.trading.signalcheck.AllocationReplayer;
import com.alphatica.alis.trading.signalcheck.SignalExecutor;
import com.alphatica.alis.trading.signalcheck.scoregenerator.ScoreCalculator;
import com.alphatica.alis.trading.signalcheck.tradesignal.SignalGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class SignalOptimizer extends Optimizer {

	private final Supplier<SignalGenerator> signalGeneratorSupplier;
	private final MarketData marketData;
	private final Time startTime;
	private final Time endTime;
	private final Predicate<TimeMarketData> marketFilter;
	private final float commissionRate;
	private final boolean tradeSecondarySignals;
	private final double maxAllocation;
	private final AllocationPolicy allocationPolicy;
	private final ScoreCalculator scoreCalculator;
	private final ParamsSelector paramsSelector;
	private final AtomicBoolean isStopped = new AtomicBoolean(false);

	private double bestScore;

	public SignalOptimizer(Supplier<SignalGenerator> signalGeneratorSupplier, MarketData marketData,
						   Time startTime, Time endTime,
						   Predicate<TimeMarketData> marketFilter, float commissionRate, boolean tradeSecondarySignals,
						   ParametersSelection parametersSelection, double maxAllocation,
						   AllocationPolicy allocationPolicy, ScoreCalculator scoreCalculator) throws OptimizerException {
		this.signalGeneratorSupplier = Objects.requireNonNull(signalGeneratorSupplier, "signalGeneratorSupplier");
		this.marketData = Objects.requireNonNull(marketData, "marketData");
		this.startTime = Objects.requireNonNull(startTime, "startTime");
		this.endTime = Objects.requireNonNull(endTime, "endTime");
		this.marketFilter = Objects.requireNonNull(marketFilter, "marketFilter");
		this.commissionRate = commissionRate;
		this.tradeSecondarySignals = tradeSecondarySignals;
		this.maxAllocation = maxAllocation;
		AllocationReplayer.validateMaxAllocation(maxAllocation);
		this.allocationPolicy = Objects.requireNonNull(allocationPolicy, "allocationPolicy");
		this.scoreCalculator = Objects.requireNonNull(scoreCalculator, "scoreCalculator");
		var fields = signalGeneratorSupplier.get().getClass().getDeclaredFields();
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
		Supplier<SignalGenerator> optimizedSignalGeneratorSupplier = () -> {
			var signalGenerator = signalGeneratorSupplier.get();
			try {
				copyParameters(nextParams, signalGenerator);
				return signalGenerator;
			} catch (IllegalAccessException e) {
				System.out.println(e);
				return null;
			}
		};
		var signalExecutor = new SignalExecutor()
				.withTimeRange(startTime, endTime)
				.withMarketFilter(marketFilter)
				.withCommissionRate(commissionRate)
				.withSecondarySignals(tradeSecondarySignals)
				.useCachedMarketData();
		var execution = signalExecutor.execute(marketData, optimizedSignalGeneratorSupplier);
		var replay = new AllocationReplayer().replay(execution, maxAllocation, allocationPolicy);
		var score = scoreCalculator.calculate(execution, replay);
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
