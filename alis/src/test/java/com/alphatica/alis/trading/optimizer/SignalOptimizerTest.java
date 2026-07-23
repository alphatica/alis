package com.alphatica.alis.trading.optimizer;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.TestData;
import com.alphatica.alis.trading.optimizer.params.BoolParam;
import com.alphatica.alis.trading.signalcheck.AllocationPolicy;
import com.alphatica.alis.trading.signalcheck.BuySignal;
import com.alphatica.alis.trading.signalcheck.tradesignal.SignalGenerator;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalOptimizerTest {
	@Test
	void shouldUseExplicitReplayConfigurationAndSelectTheBestSignalParameters() throws OptimizerException {
		AtomicInteger scores = new AtomicInteger();
		var optimizer = new SignalOptimizer(OptimizedSignalGenerator::new, new TestData("market"),
				new Time(10), new Time(13), ignored -> true, 0.0f, false,
				ParametersSelection.FULL_PERMUTATION, 1.0,
				AllocationPolicy.STOP_ON_FIRST_REJECTION, (execution, replay) -> {
					assertEquals(1.0, replay.maxAllocation());
					assertEquals(AllocationPolicy.STOP_ON_FIRST_REJECTION, replay.policy());
					scores.incrementAndGet();
					return replay.acceptedTradeCount();
				});
		PrintStream originalOut = System.out;
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			System.setOut(new PrintStream(output));
			optimizer.run();
		} finally {
			System.setOut(originalOut);
		}

		assertEquals(2, scores.get());
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
