package com.alphatica.alis.studio.logic.trading.walkforward;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.studio.logic.trading.backtest.BacktestSettings;
import com.alphatica.alis.studio.logic.trading.walkforward.WalkForward.OptimizedWindow;
import com.alphatica.alis.studio.logic.trading.walkforward.WalkForward.Result;
import com.alphatica.alis.studio.logic.trading.walkforward.WalkForward.TestedWindow;
import com.alphatica.alis.studio.logic.trading.walkforward.WalkForwardWindows.Window;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.PositionPricesRecord;
import com.alphatica.alis.trading.account.scorer.NetAssetValue;
import com.alphatica.alis.trading.optimizer.ParametersSelection;
import com.alphatica.alis.trading.optimizer.params.IntParam;
import com.alphatica.alis.trading.order.Direction;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.order.OrderSize;
import com.alphatica.alis.trading.strategy.Strategy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.IntToDoubleFunction;

import static com.alphatica.alis.trading.optimizer.ParametersSelection.FULL_PERMUTATION;
import static com.alphatica.alis.trading.optimizer.ParametersSelection.GENETIC;
import static com.alphatica.alis.trading.optimizer.ParametersSelection.RANDOM;
import static com.alphatica.alis.trading.optimizer.ResultVerifier.NONE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalkForwardTest {
	private static final double DELTA = 1e-9;
	// a rises in 2019 and 2021 and falls in 2020; b the other way round
	private static final IntToDoubleFunction A = year -> year == 2020 ? 0.998 : 1.002;
	private static final IntToDoubleFunction B = year -> year == 2020 ? 1.002 : 0.998;
	private static final List<Window> TWO_YEARS = WalkForwardWindows.of(new Time(20200101), new Time(20211231), 12, 12);

	private final List<String> events = new ArrayList<>();
	private final List<Time> sessions = new ArrayList<>();

	@Test
	void shouldTradeEveryTestWindowWithParametersBestInItsTraining() throws Exception {
		Result result = walkForward(TestMarkets.of(Map.of("a", A, "b", B))).run(listener(null), (time, account, orders) -> sessions.add(time));

		// Trained on 2019: a; trained on 2020: b
		List<OptimizedWindow> optimized = result.windows().stream().map(TestedWindow::optimized).toList();
		assertEquals(List.of(Map.of("market", 0), Map.of("market", 1)), optimized.stream().map(OptimizedWindow::params).toList());
		assertEquals(List.of(List.of("market = 0; "), List.of("market = 1; ")),
				optimized.stream().map(OptimizedWindow::paramsCode).toList());
		assertEquals(List.of(2, 2), optimized.stream().map(OptimizedWindow::iterations).toList());
		assertTrue(optimized.stream().allMatch(window -> window.trainingScore() > 100_000));
		assertEquals(List.of("start 1 of 2", "optimized 1", "start 2 of 2", "optimized 2"),
				events.stream().filter(e -> !e.startsWith("iterations")).toList());
		assertTrue(events.contains("iterations 2"));
	}

	@Test
	void shouldKeepOpenPositionsWhenParametersChange() throws Exception {
		Result result = walkForward(TestMarkets.of(Map.of("a", A, "b", B))).run(listener(null), (time, account, orders) -> {
		});

		// a, bought on the first session after the first order, is held on after b becomes the market to buy
		Account account = result.backtest().account();
		List<PositionPricesRecord> lots = account.getAllPricesRecords().stream()
				.sorted(Comparator.comparing(lot -> lot.entryTime().time())).toList();
		assertEquals(List.of(new MarketName("a"), new MarketName("b")), lots.stream().map(PositionPricesRecord::marketName).toList());
		assertEquals(List.of(new Time(20200102), new Time(20210104)), lots.stream().map(PositionPricesRecord::entryTime).toList());
		// Both sold only with the account at the end
		assertTrue(lots.stream().allMatch(lot -> lot.exitTime() == null));
		// One buy in each window; positions sold with the account at the end are no transactions of the strategy
		assertEquals(List.of(1, 1), result.windows().stream().map(TestedWindow::transactions).toList());
	}

	@Test
	void shouldCountBuysAndSellsOfEveryTestWindow() throws Exception {
		BacktestSettings settings = new BacktestSettings(new Time(0), new Time(0), 0.001, 100_000, SwitchChosen::new);
		WalkForward walkForward = new WalkForward(settings, TestMarkets.of(Map.of("a", A, "b", B)), NetAssetValue::new, NONE,
				FULL_PERMUTATION, 2, TWO_YEARS);

		Result result = walkForward.run(listener(null), (time, account, orders) -> {
		});

		// 2020: buy a; 2021: sell a, buy b
		assertEquals(List.of(1, 2), result.windows().stream().map(TestedWindow::transactions).toList());
	}

	@Test
	void shouldGiveReturnOfEveryTestWindow() throws Exception {
		Result result = walkForward(TestMarkets.of(Map.of("a", A, "b", B))).run(listener(null), (time, account, orders) -> {
		});

		List<Time> times = result.backtest().account().getAccountHistory().getSessions().times();
		double[] nav = result.backtest().account().getAccountHistory().getSessions().nav();
		int endOf2020 = times.indexOf(new Time(20201231));
		// No deposits: the return of a window is the change of NAV over it
		assertEquals((nav[endOf2020] / 100_000 - 1) * 100, result.windows().get(0).returnPercent(), DELTA);
		assertEquals((nav[nav.length - 1] / nav[endOf2020] - 1) * 100, result.windows().get(1).returnPercent(), DELTA);
		assertTrue(result.windows().get(0).returnPercent() < 0);
		assertEquals(new Time(20200101), times.getFirst());
		assertEquals(new Time(20211231), times.getLast());
	}

	@Test
	void shouldBacktestOnlyWindowsOptimizedBeforeStop() throws Exception {
		MarketData data = TestMarkets.of(Map.of("a", A, "b", B));
		WalkForward walkForward = walkForward(data);

		Result result = walkForward.run(listener(walkForward), (time, account, orders) -> sessions.add(time));

		assertEquals(1, result.windows().size());
		assertEquals(new Time(20201231), sessions.getLast());
		assertEquals(List.of("start 1 of 2", "optimized 1", "start 2 of 2"),
				events.stream().filter(e -> !e.startsWith("iterations")).toList());
	}

	@Test
	void shouldGiveNoBacktestWhenStoppedBeforeFirstWindow() throws Exception {
		WalkForward walkForward = walkForward(TestMarkets.of(Map.of("a", A, "b", B)));
		walkForward.stop();

		Result result = walkForward.run(listener(null), (time, account, orders) -> sessions.add(time));

		assertEquals(List.of(), result.windows());
		assertNull(result.backtest());
		assertEquals(List.of(), sessions);
	}

	@Test
	void shouldTakeFirstParametersByCodeOfEqualScores() throws Exception {
		// Equal markets give equal scores
		Result result = walkForward(TestMarkets.of(Map.of("a", A, "b", A))).run(listener(null), (time, account, orders) -> {
		});

		assertEquals(List.of(Map.of("market", 0), Map.of("market", 0)),
				result.windows().stream().map(window -> window.optimized().params()).toList());
	}

	@Test
	void shouldScoreBestParametersOfPreviousWindowWithRandomSelection() throws Exception {
		List<List<Integer>> scored = new ArrayList<>();
		WalkForward walkForward = recordingWalkForward(RANDOM, 1, scored);

		Result result = walkForward.run(recordingListener(scored), (time, account, orders) -> {
		});

		assertEquals(2, scored.size());
		assertEquals(List.of(scored.get(0).getFirst()), scored.get(1));
		assertEquals(result.windows().get(0).optimized().params(), result.windows().get(1).optimized().params());
	}

	@Test
	void shouldScoreBestParametersOfPreviousWindowOnceWithGeneticSelection() throws Exception {
		List<List<Integer>> scored = new ArrayList<>();
		WalkForward walkForward = recordingWalkForward(GENETIC, 4, scored);

		Result result = walkForward.run(recordingListener(scored), (time, account, orders) -> {
		});

		int previousBest = (int) result.windows().get(0).optimized().params().get("value");
		assertEquals(4, scored.get(0).size());
		assertEquals(4, scored.get(1).size());
		assertEquals((int) Collections.max(scored.get(0)), previousBest);
		assertEquals(previousBest, scored.get(1).getFirst());
		assertEquals(Collections.max(scored.get(1)), result.windows().get(1).optimized().params().get("value"));
	}

	@Test
	void shouldNotRepeatPreviousParametersWithFullPermutation() throws Exception {
		List<List<Integer>> scored = new ArrayList<>();
		WalkForward walkForward = recordingWalkForward(FULL_PERMUTATION, 100, scored);

		walkForward.run(recordingListener(scored), (time, account, orders) -> {
		});

		assertEquals(100, scored.get(1).size());
		assertEquals(100, new HashSet<>(scored.get(1)).size());
	}

	private static WalkForward recordingWalkForward(ParametersSelection selection, long iterations,
													List<List<Integer>> scored) {
		BacktestSettings settings = new BacktestSettings(new Time(0), new Time(0), 0.001, 100_000, Recorded::new);
		return new WalkForward(settings, TestMarkets.of(Map.of("a", A)), () -> (account, stats) -> {
			int value = stats.get("value").intValue();
			synchronized (scored) {
				scored.getLast().add(value);
			}
			return value;
		}, NONE, selection, iterations, TWO_YEARS);
	}

	private static WalkForward.Listener recordingListener(List<List<Integer>> scored) {
		return new WalkForward.Listener() {
			@Override
			public void windowStarted(Window window, int windows) {
				synchronized (scored) {
					scored.add(new ArrayList<>());
				}
			}

			@Override
			public void iterations(int iterations) {
			}

			@Override
			public void windowOptimized(OptimizedWindow window) {
			}
		};
	}

	static final class Recorded extends Strategy {
		@IntParam(start = 0, step = 1, end = 99)
		int value;

		@Override
		public List<Order> afterClose(TimeMarketDataSet data, Account account) {
			return List.of();
		}

		@Override
		public Map<String, Double> getCustomStats() {
			return Map.of("value", (double) value);
		}
	}

	private static WalkForward walkForward(MarketData data) {
		BacktestSettings settings = new BacktestSettings(new Time(0), new Time(0), 0.001, 100_000, BuyChosen::new);
		return new WalkForward(settings, data, NetAssetValue::new, NONE, FULL_PERMUTATION, 2, TWO_YEARS);
	}

	/** Records the events; stops the walk-forward when the second window starts, if given. */
	private WalkForward.Listener listener(WalkForward stopAtSecond) {
		return new WalkForward.Listener() {
			@Override
			public void windowStarted(Window window, int windows) {
				events.add("start " + window.number() + " of " + windows);
				if (stopAtSecond != null && window.number() == 2) {
					stopAtSecond.stop();
				}
			}

			@Override
			public void iterations(int iterations) {
				events.add("iterations " + iterations);
			}

			@Override
			public void windowOptimized(OptimizedWindow window) {
				events.add("optimized " + window.window().number());
			}
		};
	}

	/** Holds only the chosen market: sells the other one, buys 40 % of NAV of the chosen one when it holds none of it. */
	static final class SwitchChosen extends Strategy {
		@IntParam(start = 0, step = 1, end = 1)
		int market;

		@Override
		public List<Order> afterClose(TimeMarketDataSet data, Account account) {
			MarketName chosen = BuyChosen.MARKETS.get(market);
			List<Order> orders = new ArrayList<>();
			for (MarketName other : BuyChosen.MARKETS) {
				if (!other.equals(chosen) && account.getPosition(other) != null) {
					orders.add(new Order(other, Direction.SELL, OrderSize.PERCENTAGE, 100, 1));
				}
			}
			if (account.getPosition(chosen) == null && data.get(chosen) != null) {
				orders.add(new Order(chosen, Direction.BUY, OrderSize.PERCENTAGE, 40, 1));
			}
			return orders;
		}
	}

	/** Buys 40 % of NAV of the chosen market when it holds none of it; never sells. */
	static final class BuyChosen extends Strategy {
		private static final List<MarketName> MARKETS = List.of(new MarketName("a"), new MarketName("b"));

		@IntParam(start = 0, step = 1, end = 1)
		int market;

		@Override
		public List<Order> afterClose(TimeMarketDataSet data, Account account) {
			MarketName chosen = MARKETS.get(market);
			if (account.getPosition(chosen) != null || data.get(chosen) == null) {
				return List.of();
			}
			return List.of(new Order(chosen, Direction.BUY, OrderSize.PERCENTAGE, 40, 1));
		}
	}
}
