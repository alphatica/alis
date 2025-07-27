package com.alphatica.alis.trading.signalcheck;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.TestData;
import com.alphatica.alis.trading.signalcheck.scoregenerator.ScoreGenerator;
import com.alphatica.alis.trading.signalcheck.tradesignal.TradeSignal;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;
import static com.alphatica.alis.tools.java.NumberTools.percentChange;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SignalCheckTest {

	@Test
	void shouldExecuteSignalForPrimaryTrades() {
		var scoreGenerator = new TestScoreGenerator();
		var data = new TestData();
		var executor = new SignalExecutor(signalSupplier(), new Time(10), new Time(20), data, STOCKS, 0.01f, false, scoreGenerator);
		var score = executor.execute();
		assertEquals(50.80, score, 0.01);
		assertEquals(1, scoreGenerator.trades.get());
	}

	@Test
	void shouldExecuteSignalForSecondaryTrades() {
		var scoreGenerator = new TestScoreGenerator();
		var data = new TestData();
		var executor = new SignalExecutor(signalSupplier(), new Time(10), new Time(20), data, STOCKS, 0.01f, true, scoreGenerator);
		var score = executor.execute();
		assertEquals(159.36, score, 0.01);
		assertEquals(5, scoreGenerator.trades.get());
	}

	private Supplier<TradeSignal> signalSupplier() {
		return () -> new TradeSignal() {
			@Override
			public float shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
				if (marketData.getTime().isAfter(new Time(11)) && marketData.getTime().isBefore(new Time(17))) {
					return 1.0f;
				} else {
					return Float.NaN;
				}
			}

			@Override
			public boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
				 return marketData.getTime().isAfter(new Time(18));
			}
		};
	}
}

class TestScoreGenerator extends ScoreGenerator {
	AtomicInteger trades = new AtomicInteger(0);
	volatile double profit;

	@Override
	public void afterTrade(OpenTrade trade, float effectiveClosePrice) {
		trades.incrementAndGet();
		synchronized (this) {
			profit += percentChange(trade.getOpenPrice(), effectiveClosePrice);
		}
	}

	@Override
	public double score() {
		return profit;
	}
}
