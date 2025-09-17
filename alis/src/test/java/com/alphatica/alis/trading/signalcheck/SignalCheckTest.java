package com.alphatica.alis.trading.signalcheck;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.TestData;
import com.alphatica.alis.trading.ranking.PositionReporter;
import com.alphatica.alis.trading.signalcheck.scoregenerator.ArithmeticAverageProfitPerBarScoreGenerator;
import com.alphatica.alis.trading.signalcheck.scoregenerator.ScoreGenerator;
import com.alphatica.alis.trading.signalcheck.tradesignal.TradeSignal;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;
import static com.alphatica.alis.tools.java.NumberTools.percentChange;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SignalCheckTest {

	@Test
	void shouldExecuteSignalForPrimaryTrades() {
		var scoreGenerator = new TestScoreGenerator();
		var data = new TestData("test_market");
		var positionReporter = new PositionReporter();
		var executor = new SignalExecutor(TestSignal::new, new Time(10), new Time(20), data, STOCKS, 0.01f, false, scoreGenerator);
		executor.withPositionReporter(positionReporter, "s1");
		var score = executor.execute();
		assertEquals(50.80, score, 0.01);
		assertEquals(1, scoreGenerator.trades.get());
	}

	@Test
	void shouldExecuteSignalForSecondaryTrades() {
		var scoreGenerator = new TestScoreGenerator();
		var data = new TestData("test_market");
		var executor = new SignalExecutor(TestSignal::new, new Time(10), new Time(20), data, STOCKS, 0.01f, true, scoreGenerator);
		var score = executor.execute();
		assertEquals(159.36, score, 0.01);
		assertEquals(5, scoreGenerator.trades.get());
	}

	@Test
	void shouldReportTrades() {
		var data = new TestData("market1", "market2");
		var positionReporter = new PositionReporter();
		var executor = new SignalExecutor(TestSignal::new, new Time(10), new Time(20), data, STOCKS, 0.0f, true, new ArithmeticAverageProfitPerBarScoreGenerator());
		executor.withPositionReporter(positionReporter, "s1");
		executor.execute();
		var positionReports = positionReporter.getContent();
		assertNull(positionReports.get(new Time(13)));
		assertEquals(1, positionReports.get(new Time(14)).get("s1").get(new MarketName("market1")).doubleValue());
		assertEquals(2, positionReports.get(new Time(15)).get("s1").get(new MarketName("market1")).doubleValue());
		assertEquals(3, positionReports.get(new Time(16)).get("s1").get(new MarketName("market1")).doubleValue());
		assertEquals(6, positionReports.get(new Time(16)).get("s1").get(new MarketName("market2")).doubleValue());
		assertEquals(10, positionReports.get(new Time(20)).get("s1").get(new MarketName("market2")).doubleValue());
		assertNull(positionReports.get(new Time(21)));
	}

}

class TestSignal extends TradeSignal {
	// Opens position on 13th, 14th, 15th, 16th and 17th
	@Override
	public float shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
		if (marketData.getTime().isAfter(new Time(11)) && marketData.getTime().isBefore(new Time(17))) {
			if (marketData.getMarketName().name().equals("market1")) {
				return 1.0f;
			} else {
				return 2.0f;
			}

		} else {
			return Float.NaN;
		}
	}

	// Closes all on 20th
	@Override
	public boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
		return marketData.getTime().isAfter(new Time(18));
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
