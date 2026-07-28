package com.alphatica.alis.trading.datamining.betterexits;

import com.alphatica.alis.data.FloatArraySlice;
import com.alphatica.alis.data.StandardMarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.market.MarketType;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.TestData;
import com.alphatica.alis.tools.data.TestMarket;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.Deposit;
import com.alphatica.alis.trading.account.actions.Trade;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BetterExitSimulationTest {

	private static final MarketName MARKET = new MarketName("market");
	private static final MarketName OTHER_MARKET = new MarketName("other");

	@Test
	void shouldResetFinderStateWhenOriginalSellClosesPositionBeforeReentry()
			throws AccountActionException {
		List<Double> observedStateValues = new ArrayList<>();
		BetterExitFinder finder = finderRecordingState(observedStateValues);
		List<AccountAction> actions = List.of(
				new AccountAction(new Time(1), new Deposit(100)),
				new AccountAction(new Time(1), new Trade(MARKET, BUY, 1, 10, 0)),
				new AccountAction(new Time(2), new Trade(MARKET, SELL, 2, 10, 0)),
				new AccountAction(new Time(2), new Trade(MARKET, BUY, 2, 10, 0)),
				new AccountAction(new Time(4), new Trade(MARKET, SELL, 4, 10, 0)));

		BetterExitSimulation simulation = new BetterExitSimulation(
				new TestData(MARKET.toString()), actions, finder, 0);

		assertTrue(simulation.run().isPresent());
		assertEquals(List.of(1.0, 1.0, 2.0), observedStateValues);
	}

	@Test
	void shouldNotRepeatExitSignalWhileWaitingForNextMarketSession()
			throws AccountActionException {
		AtomicInteger finderCalls = new AtomicInteger();
		BetterExitFinder finder = finderWithResult(finderCalls, true);
		BetterExitSimulation simulation = new BetterExitSimulation(
				marketDataWithGap(), fundedPositionAcrossGap(), finder, 0);

		BetterExitSimulationResult result = simulation.run().orElseThrow();

		assertEquals(1, result.trades());
		assertEquals(1, finderCalls.get());
		assertTrue(result.account().getPositions().isEmpty());
	}

	@Test
	void shouldNotEvaluatePositionUsingStaleMarketData() throws AccountActionException {
		AtomicInteger finderCalls = new AtomicInteger();
		BetterExitFinder finder = finderWithResult(finderCalls, false);
		BetterExitSimulation simulation = new BetterExitSimulation(
				marketDataWithGap(), fundedPositionAcrossGap(), finder, 0);

		BetterExitSimulationResult result = simulation.run().orElseThrow();

		assertEquals(0, result.trades());
		assertEquals(1, finderCalls.get());
	}

	private static BetterExitFinder finderRecordingState(List<Double> observedStateValues) {
		return new BetterExitFinder() {
			@Override
			public boolean shouldExit(
					Account account,
					TimeMarketData marketData,
					TimeMarketDataSet allData,
					MarketStateSet marketStateSet) {
				DoubleValueState state = marketStateSet.get(marketData.getMarketName());
				state.value++;
				observedStateValues.add(state.value);
				return false;
			}

			@Override
			public String name() {
				return "state-recording finder";
			}

			@Override
			public String description() {
				return "Records finder state";
			}
		};
	}

	private static BetterExitFinder finderWithResult(AtomicInteger calls, boolean shouldExit) {
		return new BetterExitFinder() {
			@Override
			public boolean shouldExit(
					Account account,
					TimeMarketData marketData,
					TimeMarketDataSet allData,
					MarketStateSet marketStateSet) {
				calls.incrementAndGet();
				return shouldExit;
			}

			@Override
			public String name() {
				return "fixed-result finder";
			}

			@Override
			public String description() {
				return "Returns a fixed result";
			}
		};
	}

	private static List<AccountAction> fundedPositionAcrossGap() {
		return List.of(
				new AccountAction(new Time(1), new Deposit(100)),
				new AccountAction(new Time(1), new Trade(MARKET, BUY, 1, 10, 0)),
				new AccountAction(new Time(4), new Trade(MARKET, SELL, 4, 10, 0)));
	}

	private static StandardMarketData marketDataWithGap() {
		StandardMarketData marketData = new StandardMarketData();
		marketData.addMarkets(Map.of(
				MARKET, market(MARKET, 1, 4),
				OTHER_MARKET, market(OTHER_MARKET, 2, 3)));
		return marketData;
	}

	private static TestMarket market(MarketName name, int... times) {
		TreeMap<Time, TimeMarketData> data = new TreeMap<>();
		for (int time : times) {
			FloatArraySlice prices = new FloatArraySlice(new float[]{time}, 0);
			Time marketTime = new Time(time);
			data.put(marketTime, new TimeMarketData(
					name,
					MarketType.STOCK,
					marketTime,
					List.of(prices, prices, prices, prices)));
		}
		return new TestMarket(name, data);
	}
}
