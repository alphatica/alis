package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.data.market.MarketData;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

public class SignalOptimizer extends Optimizer {

	private final Supplier<SignalGenerator> signalGeneratorFactory;
	private final MarketData marketData;
	private final Supplier<SignalExecutor> executorFactory;
	private final Supplier<ScoreCalculator> scorerFactory;
	private final double maxAllocation;
	private final AllocationPolicy allocationPolicy;
	private final ParamsSelector paramsSelector;
	private final AtomicBoolean isStopped = new AtomicBoolean(false);

	private double bestScore;

	public SignalOptimizer(Supplier<SignalGenerator> signalGeneratorFactory, MarketData marketData,
						   Supplier<SignalExecutor> executorFactory, Supplier<ScoreCalculator> scorerFactory,
						   AllocationPolicy allocationPolicy, ParametersSelection parametersSelection,
						   double maxAllocation) throws OptimizerException {
		this.signalGeneratorFactory = requireNonNull(signalGeneratorFactory, "signalGeneratorFactory");
		this.marketData = requireNonNull(marketData, "marketData");
		this.executorFactory = requireNonNull(executorFactory, "executorFactory");
		this.scorerFactory = requireNonNull(scorerFactory, "scorerFactory");
		this.maxAllocation = maxAllocation;
		AllocationReplayer.validateMaxAllocation(maxAllocation);
		this.allocationPolicy = requireNonNull(allocationPolicy, "allocationPolicy");
		requireNonNull(parametersSelection, "parametersSelection");
		var fields = signalGeneratorFactory.get().getClass().getDeclaredFields();
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
			var signalGenerator = signalGeneratorFactory.get();
			try {
				copyParameters(nextParams, signalGenerator);
				return signalGenerator;
			} catch (IllegalAccessException e) {
				System.out.println(e);
				return null;
			}
		};
		var signalExecutor = requireNonNull(executorFactory.get(), "executorFactory result");
		var execution = signalExecutor.execute(marketData, optimizedSignalGeneratorSupplier);
		var replay = new AllocationReplayer().replay(execution, maxAllocation, allocationPolicy);
		var scoreCalculator = requireNonNull(scorerFactory.get(), "scorerFactory result");
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
