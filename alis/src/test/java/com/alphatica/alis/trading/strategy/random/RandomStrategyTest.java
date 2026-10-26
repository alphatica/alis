package com.alphatica.alis.trading.strategy.random;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.tools.data.TestData;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.PositionHistoryRecord;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.strategy.StrategyExecutor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RandomStrategyTest {

	@Test
	void shouldHoldPositionsForGivenLengths() throws AccountActionException {
		RandomStrategy strategy = new RandomStrategy();
		strategy.setTradeProbability(1.0);
		strategy.setHoldingLengths(new int[]{7});
		strategy.setPositionSizePercent(10);

		Account account = new StrategyExecutor().withTimeRange(new Time(1), new Time(200))
				.execute(new TestData("a", "b"), strategy);

		// Positions closed by the end of the backtest have no exit time and may be shorter
		List<PositionHistoryRecord> sold = account.getAllHistoryRecords().stream()
				.filter(r -> !r.open() && r.exitTime() != null).toList();
		assertFalse(sold.isEmpty());
		sold.forEach(r -> assertEquals(7, r.tradeLength()));
	}

	@Test
	void shouldReportHowOftenRandomTradingWasBetter() throws AccountActionException {
		RandomStrategy base = new RandomStrategy();
		base.setTradeProbability(0.2);
		base.setHoldingLengths(new int[]{5});
		TestData data = new TestData("a", "b", "c");
		Time start = new Time(1);
		Time end = new Time(300);
		Account baseAccount = new StrategyExecutor().withTimeRange(start, end).withInitialCash(100_000).withCommissionRate(0.001)
				.execute(data, base);
		List<String> lines = new ArrayList<>();

		new RandomStrategyComparator(lines::add, 20, 100_000, 0.001, start, end, data).compare(baseAccount);

		assertTrue(lines.getLast().startsWith("RandomStrategy achieved better result "), lines.getLast());
		assertTrue(lines.getLast().contains(" out of 20 "), lines.getLast());
		assertTrue(lines.stream().anyMatch(l -> l.startsWith("Backtest: ")));
		assertTrue(lines.stream().anyMatch(l -> l.startsWith("Random trading: ")));
	}
}
