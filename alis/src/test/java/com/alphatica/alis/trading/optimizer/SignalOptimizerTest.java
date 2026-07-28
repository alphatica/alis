package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.TestData;
import com.alphatica.alis.trading.optimizer.params.BoolParam;
import com.alphatica.alis.trading.signalcheck.AllocationPolicy;
import com.alphatica.alis.trading.signalcheck.BuySignal;
import com.alphatica.alis.trading.signalcheck.SignalExecutor;
import com.alphatica.alis.trading.signalcheck.scoregenerator.ScoreCalculator;
import com.alphatica.alis.trading.signalcheck.tradesignal.SignalGenerator;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalOptimizerTest {
	@Test
	void shouldUseExplicitReplayConfigurationAndSelectTheBestSignalParameters() throws OptimizerException {
		AtomicInteger scores = new AtomicInteger();
		AtomicInteger executors = new AtomicInteger();
		AtomicInteger scorers = new AtomicInteger();
		Supplier<SignalExecutor> executorFactory = () -> {
			executors.incrementAndGet();
			return new SignalExecutor().withTimeRange(new Time(10), new Time(13));
		};
		Supplier<ScoreCalculator> scorerFactory = () -> {
			scorers.incrementAndGet();
			return (execution, replay) -> {
					assertEquals(1.0, replay.maxAllocation());
					assertEquals(AllocationPolicy.STOP_ON_FIRST_REJECTION, replay.policy());
					scores.incrementAndGet();
					return replay.acceptedTradeCount();
				};
		};
		var optimizer = new SignalOptimizer(OptimizedSignalGenerator::new, new TestData("market"),
				executorFactory, scorerFactory, AllocationPolicy.STOP_ON_FIRST_REJECTION,
				ParametersSelection.FULL_PERMUTATION, 1.0);
		PrintStream originalOut = System.out;
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(output));
			optimizer.run();
		} finally {
			System.setOut(originalOut);
		}

		assertEquals(2, scores.get());
		assertEquals(2, executors.get());
		assertEquals(2, scorers.get());
		assertTrue(output.toString().contains("enabled = true;"));
	}

	private static final class OptimizedSignalGenerator extends SignalGenerator {
		@BoolParam
		private boolean enabled;

		@Override
		public Optional<BuySignal> shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
			return enabled ? Optional.of(new BuySignal(1.0, 1.0)) : Optional.empty();
		}

		@Override
		public boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
			return false;
		}
	}
}
