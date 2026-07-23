package com.alphatica.alis.trading.signalcheck;

import com.alphatica.alis.data.StandardMarketData;
import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.FloatArraySlice;
import com.alphatica.alis.tools.data.TestData;
import com.alphatica.alis.tools.data.TestMarket;
import com.alphatica.alis.trading.ranking.PositionReporter;
import com.alphatica.alis.trading.signalcheck.tradesignal.SignalGenerator;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalCheckTest {

	@Test
	void shouldDiscoverOnePrimaryTradeWithCompleteTimelineAndCommission() {
		var executor = configuredExecutor(false, 20);
		SignalExecutionResult result = executor.execute(new TestData("market1"), TestSignalGenerator::new);

		assertEquals(new Time(10), result.startTime());
		assertEquals(new Time(20), result.endTime());
		assertEquals(11, result.executionTimes().size());
		assertEquals(1, result.opportunities().size());
		TradeOpportunity trade = result.opportunities().getFirst();
		assertEquals(new Time(12), trade.signalTime());
		assertEquals(new Time(13), trade.openTime());
		assertEquals(new Time(20), trade.closeTime());
		assertEquals(3, trade.openEventIndex());
		assertEquals(10, trade.closeEventIndex());
		assertEquals(13.13f, trade.effectiveOpenPrice(), 0.0001f);
		assertEquals(19.8f, trade.effectiveClosePrice(), 0.0001f);
		assertEquals(7, trade.bars());
		assertEquals(1.0, trade.requestedAllocation());
		assertEquals(10.0, trade.priority());
	}

	@Test
	void shouldDiscoverEverySecondaryTradeWithoutAnAllocationLimit() {
		SignalExecutionResult result = configuredExecutor(true, 20)
				.execute(new TestData("market1"), TestSignalGenerator::new);

		assertEquals(5, result.opportunities().size());
		assertEquals(java.util.List.of(new Time(12), new Time(13), new Time(14), new Time(15), new Time(16)),
				result.opportunities().stream().map(TradeOpportunity::signalTime).toList());
		assertTrue(result.opportunities().stream().allMatch(trade -> trade.closeTime().equals(new Time(20))));
	}

	@Test
	void shouldKeepPrimaryDiscoveryIndependentOfReplayRejection() {
		SignalExecutionResult result = configuredExecutor(false, 20)
				.execute(new TestData("market1"), TestSignalGenerator::new);
		AllocationReplayResult replay = new AllocationReplayer().replay(result, 0.5,
				AllocationPolicy.STOP_ON_FIRST_REJECTION);

		assertEquals(1, result.opportunities().size());
		assertTrue(replay.acceptedTrades().isEmpty());
		assertEquals(1, replay.rejectedTrades());
	}

	@Test
	void shouldForceCloseOpenAndPendingCloseTradesAfterTheFinalEvent() {
		SignalExecutionResult openResult = configuredExecutor(false, 18)
				.execute(new TestData("market1"), TestSignalGenerator::new);
		TradeOpportunity openTrade = openResult.opportunities().getFirst();
		assertEquals(9, openTrade.closeEventIndex());
		assertEquals(new Time(18), openTrade.closeTime());
		assertEquals(17.82f, openTrade.effectiveClosePrice(), 0.0001f);

		SignalExecutionResult pendingCloseResult = configuredExecutor(false, 19)
				.execute(new TestData("market1"), TestSignalGenerator::new);
		TradeOpportunity pendingTrade = pendingCloseResult.opportunities().getFirst();
		assertEquals(10, pendingTrade.closeEventIndex());
		assertEquals(new Time(19), pendingTrade.closeTime());
		assertEquals(18.81f, pendingTrade.effectiveClosePrice(), 0.0001f);
	}

	@Test
	void shouldIgnorePendingOpenAtTheEndOfTheRange() {
		SignalGenerator signalGenerator = new SignalGenerator() {
			@Override
			public Optional<BuySignal> shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
				return Optional.of(new BuySignal(1.0, 1.0));
			}

			@Override
			public boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
				return false;
			}
		};
		SignalExecutionResult result = new SignalExecutor().withTimeRange(new Time(20), new Time(20))
				.withCommissionRate(0.0).execute(new TestData("market"), () -> signalGenerator);

		assertTrue(result.opportunities().isEmpty());
	}

	@Test
	void shouldOpenOnTheNextSessionAvailableForThatMarket() {
		StandardMarketData data = new StandardMarketData();
		data.addMarkets(Map.of(
				new MarketName("a"), sparseMarket("a", 10, 12),
				new MarketName("calendar"), sparseMarket("calendar", 11)));
		SignalGenerator signalGenerator = alwaysBuyNeverSell();

		SignalExecutionResult result = new SignalExecutor()
				.withTimeRange(new Time(10), new Time(12))
				.withMarketFilter(market -> market.getMarketName().equals(new MarketName("a")))
				.withCommissionRate(0.0)
				.execute(data, () -> signalGenerator);

		TradeOpportunity opportunity = result.opportunities().getFirst();
		assertEquals(new Time(10), opportunity.signalTime());
		assertEquals(new Time(12), opportunity.openTime());
		assertEquals(2, opportunity.openEventIndex());
		assertEquals(3, opportunity.closeEventIndex());
	}

	@Test
	void shouldReportAllHypotheticalRequestedAllocationsAtTheOriginalMoment() {
		var data = new TestData("market1", "market2");
		var reporter = new PositionReporter();
		new SignalExecutor()
				.withTimeRange(new Time(10), new Time(20))
				.withMarketFilter(STOCKS)
				.withCommissionRate(0.0)
				.withSecondarySignals(true)
				.withPositionReporter(reporter, "s1")
				.execute(data, TestSignalGenerator::new);

		var reports = reporter.getContent();
		assertNull(reports.get(new Time(13)));
		assertEquals(1.0, reporter.getPosition(new Time(14), "s1", new MarketName("market1")));
		assertEquals(2.0, reporter.getPosition(new Time(15), "s1", new MarketName("market1")));
		assertEquals(3.0, reporter.getPosition(new Time(16), "s1", new MarketName("market1")));
		assertEquals(6.0, reporter.getPosition(new Time(16), "s1", new MarketName("market2")));
		assertEquals(10.0, reporter.getPosition(new Time(20), "s1", new MarketName("market2")));
		assertNull(reports.get(new Time(21)));
	}

	@Test
	void shouldBeRepeatableAndReturnImmutableDeterministicResults() {
		var executor = configuredExecutor(true, 20);
		var data = new TestData("z_market", "a_market");
		SignalExecutionResult first = executor.execute(data, TestSignalGenerator::new);
		SignalExecutionResult second = executor.execute(data, TestSignalGenerator::new);

		assertEquals(first, second);
		assertEquals(new MarketName("a_market"), first.opportunities().getFirst().market());
		assertThrowsUnsupported(() -> first.executionTimes().add(new Time(30)));
		assertThrowsUnsupported(() -> first.opportunities().clear());
	}

	@Test
	void shouldNotDependOnMarketLoadingOrder() {
		MarketData data = new TestData("z_market", "a_market", "middle_market");
		MarketData reversed = reversedMarketView(data);

		SignalExecutionResult first = configuredExecutor(true, 20).execute(data, TestSignalGenerator::new);
		SignalExecutionResult second = configuredExecutor(true, 20).execute(reversed, TestSignalGenerator::new);

		assertEquals(first, second);
	}

	private static SignalExecutor configuredExecutor(boolean secondarySignals, int endTime) {
		return new SignalExecutor()
				.withTimeRange(new Time(10), new Time(endTime))
				.withMarketFilter(STOCKS)
				.withCommissionRate(0.01)
				.withSecondarySignals(secondarySignals);
	}

	private static void assertThrowsUnsupported(Runnable runnable) {
		boolean failed = false;
		try {
			runnable.run();
		} catch (UnsupportedOperationException expected) {
			failed = true;
		}
		assertTrue(failed);
	}

	private static SignalGenerator alwaysBuyNeverSell() {
		return new SignalGenerator() {
			@Override
			public Optional<BuySignal> shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
				return Optional.of(new BuySignal(1.0, 1.0));
			}

			@Override
			public boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
				return false;
			}
		};
	}

	private static Market sparseMarket(String name, int... times) {
		MarketName marketName = new MarketName(name);
		TreeMap<Time, TimeMarketData> data = new TreeMap<>();
		for (int time : times) {
			FloatArraySlice prices = new FloatArraySlice(new float[]{time}, 0);
			data.put(new Time(time), new TimeMarketData(marketName, MarketType.STOCK, new Time(time),
					List.of(prices, prices, prices, prices)));
		}
		return new TestMarket(marketName, data);
	}

	private static MarketData reversedMarketView(MarketData delegate) {
		return new MarketData() {
			@Override
			public List<Time> getTimes() {
				return delegate.getTimes();
			}

			@Override
			public Market getMarket(MarketName marketName) {
				return delegate.getMarket(marketName);
			}

			@Override
			public List<Market> listMarkets(Predicate<Market> filter) {
				return delegate.listMarkets(filter).reversed();
			}
		};
	}
}

class TestSignalGenerator extends SignalGenerator {
	@Override
	public Optional<BuySignal> shouldBuy(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
		if (marketData.getTime().isAfter(new Time(11)) && marketData.getTime().isBefore(new Time(17))) {
			double allocation = marketData.getMarketName().name().equals("market1") ? 1.0 : 2.0;
			return Optional.of(new BuySignal(allocation, 10.0));
		}
		return Optional.empty();
	}

	@Override
	public boolean shouldSell(TimeMarketData marketData, TimeMarketDataSet marketDataSet) {
		return marketData.getTime().isAfter(new Time(18));
	}
}
