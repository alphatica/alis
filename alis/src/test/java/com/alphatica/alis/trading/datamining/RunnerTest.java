package com.alphatica.alis.trading.datamining;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.data.TestData;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.Deposit;
import com.alphatica.alis.trading.account.actions.Trade;
import com.alphatica.alis.trading.datamining.betterexits.BetterExitFinder;
import com.alphatica.alis.trading.datamining.betterexits.ExitFinderResult;
import com.alphatica.alis.trading.datamining.betterexits.MarketStateSet;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunnerTest {

	private static final MarketName MARKET = new MarketName("market");

	@Test
	void shouldReplayOriginalExitAndReportScore() throws AccountActionException {
		ExitFinderResult result = run(new Runner(), finder(data -> false), fundedRoundTrip());

		assertEquals(0, result.trades());
		assertEquals("test finder", result.name());
		assertEquals("test description", result.description());
		assertEquals(140.0, result.score(), 0.000_001);
		assertEquals(result.account().getCash(), result.score(), 0.000_001);
		assertTrue(result.account().getPositions().isEmpty());
	}

	@Test
	void shouldSellOnNextSessionAndRemoveOriginalSell() throws AccountActionException {
		BetterExitFinder exitAtTimeTwo = finder(data -> data.getTime().equals(new Time(2)));

		ExitFinderResult result = run(new Runner(), exitAtTimeTwo, fundedRoundTrip());

		assertEquals(1, result.trades());
		assertEquals(119.7, result.account().getCash(), 0.000_001);
		assertTrue(result.account().getPositions().isEmpty());
	}

	@Test
	void shouldRepayTemporaryCashAddedForBuy() throws AccountActionException {
		List<AccountAction> actions = List.of(
				new AccountAction(new Time(1), new Trade(MARKET, BUY, 1, 10, 0)),
				new AccountAction(new Time(5), new Trade(MARKET, SELL, 5, 10, 0)));

		ExitFinderResult result = run(new Runner(), finder(data -> false), actions);

		assertEquals(40.0, result.account().getCash(), 0.000_001);
		assertEquals(0.0, result.account().getAccountHistory().getCashPayments(), 0.000_001);
	}

	@Test
	void shouldNotCarryTradeCountBetweenRuns() throws AccountActionException {
		Runner runner = new Runner();
		ExitFinderResult first = run(
				runner,
				finder(data -> data.getTime().equals(new Time(2))),
				fundedRoundTrip());
		ExitFinderResult second = run(runner, finder(data -> false), fundedRoundTrip());

		assertEquals(1, first.trades());
		assertEquals(0, second.trades());
	}

	private static List<AccountAction> fundedRoundTrip() {
		return List.of(
				new AccountAction(new Time(1), new Deposit(100)),
				new AccountAction(new Time(1), new Trade(MARKET, BUY, 1, 10, 0)),
				new AccountAction(new Time(5), new Trade(MARKET, SELL, 5, 10, 0)));
	}

	private static ExitFinderResult run(
			Runner runner,
			BetterExitFinder finder,
			List<AccountAction> actions) throws AccountActionException {
		AtomicReference<ExitFinderResult> result = new AtomicReference<>();
		runner.run(
				new TestData(MARKET.toString()),
				actions,
				List.of(() -> finder),
				() -> (account, customStats) -> account.getCash(),
				result::set);
		ExitFinderResult value = result.get();
		assertNotNull(value);
		return value;
	}

	private static BetterExitFinder finder(Predicate<TimeMarketData> exitCondition) {
		return new BetterExitFinder() {
			@Override
			public boolean shouldExit(
					Account account,
					TimeMarketData marketData,
					TimeMarketDataSet allData,
					MarketStateSet marketStateSet) {
				return exitCondition.test(marketData);
			}

			@Override
			public String name() {
				return "test finder";
			}

			@Override
			public String description() {
				return "test description";
			}
		};
	}
}
