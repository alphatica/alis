package com.alphatica.alis.studio.view.window.analysis;

import com.alphatica.alis.data.market.Market;
import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.Deposit;
import com.alphatica.alis.trading.account.actions.Trade;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import static com.alphatica.alis.trading.order.Direction.BUY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OriginalAccountReplayerTest {

	private static final MarketName MARKET = new MarketName("market");
	private static final Time TIME = new Time(1);

	@Test
	void shouldCloseOpenPositionsUsingSimulationCommissionRate()
			throws AccountActionException {
		List<AccountAction> actions = List.of(
				new AccountAction(TIME, new Deposit(100)),
				new AccountAction(TIME, new Trade(MARKET, BUY, 10, 5, 0)));

		Account account = OriginalAccountReplayer.replay(marketData(), actions, 0.01);

		assertTrue(account.getPositions().isEmpty());
		assertEquals(99.5, account.getCash(), 0.000_001);
		assertEquals(0.5, account.getAccountHistory().getPaidCommissions(), 0.000_001);
	}

	private static MarketData marketData() {
		return new MarketData() {
			@Override
			public List<Time> getTimes() {
				return List.of(TIME);
			}

			@Override
			public Market getMarket(MarketName marketName) {
				return null;
			}

			@Override
			public List<Market> listMarkets(Predicate<Market> filter) {
				return List.of();
			}
		};
	}
}
