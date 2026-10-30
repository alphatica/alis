package com.alphatica.alis.studio.logic.trading.walkforward;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.optimizer.params.IntParam;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WalkForwardStrategyTest {

	@Test
	void shouldSwitchParametersOnFirstSessionOfEveryDayAndKeepState() {
		Recording recording = new Recording();
		WalkForwardStrategy strategy = new WalkForwardStrategy(recording,
				Map.of(new Time(20200106), Map.of("level", 1), new Time(20200108), Map.of("level", 3)));

		// 8 January is a holiday here: the second parameters start on 9 January
		for (int day : new int[]{20200106, 20200107, 20200109, 20200110}) {
			strategy.afterClose(new TimeMarketDataSet(Map.of(), new Time(day)), new Account(1000));
		}

		assertEquals(List.of(1, 1, 3, 3), recording.levels);
		// Set once per new parameters, the state goes on
		assertEquals(2, recording.changes);
		assertEquals(4, recording.sessions);
	}

	@Test
	void shouldRejectSessionBeforeFirstParameters() {
		WalkForwardStrategy strategy = new WalkForwardStrategy(new Recording(), Map.of(new Time(20200106), Map.of("level", 1)));

		assertThrows(IllegalStateException.class,
				() -> strategy.afterClose(new TimeMarketDataSet(Map.of(), new Time(20200103)), new Account(1000)));
		assertThrows(IllegalArgumentException.class, () -> new WalkForwardStrategy(new Recording(), Map.of()));
	}

	@Test
	void shouldPassEndOfTradingOn() {
		Recording recording = new Recording();

		new WalkForwardStrategy(recording, Map.of(new Time(20200106), Map.of("level", 1))).finished(new Account(1000));

		assertEquals(1, recording.finished);
	}

	/** Records the parameter it trades with at every session. */
	static final class Recording extends Strategy {
		@IntParam(start = 1, step = 1, end = 5)
		int level;
		final List<Integer> levels = new ArrayList<>();
		int changes;
		int sessions;
		int finished;

		@Override
		public void paramsChanged() {
			changes++;
		}

		@Override
		public List<Order> afterClose(TimeMarketDataSet data, Account account) {
			levels.add(level);
			sessions++;
			return List.of();
		}

		@Override
		public void finished(Account account) {
			finished++;
		}
	}
}
