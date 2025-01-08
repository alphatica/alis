package com.alphatica.alis.trading.account.actions;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.AccountHistory;
import com.alphatica.alis.trading.account.TradeStats;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;
import static org.junit.jupiter.api.Assertions.*;

class AccountActionTest {

	@Test
	void shouldPerformActions() throws AccountActionException {
		List<AccountAction> actions = new ArrayList<>(List.of(
					new AccountAction(new Time(1), new Deposit(10000)),
					new AccountAction(new Time(2), new Trade(new MarketName("1"), BUY, 10, 100, 1)),
					// Spent 2000 cash on position
					new AccountAction(new Time(4), new Trade(new MarketName("2"), BUY, 10, 200, 2)),
					new AccountAction(new Time(4), new Trade(new MarketName("3"), BUY, 10, 200, 2)),
					// Position 1 closed with profit 100%, 1000 cash
					new AccountAction(new Time(5), new Trade(new MarketName("1"), SELL, 20, 100, 3)),
					// Position 3 closed with loss 50%, 1000 cash
					new AccountAction(new Time(5), new Trade(new MarketName("3"), SELL, 5, 200, 3)),
					// Position 2 partially closed with 2000 cash back
					new AccountAction(new Time(5), new Trade(new MarketName("2"), SELL, 20, 100, 4)),
					// Position 2 closed completely with 1000 cash back
					// Total cash back from position 2 = 3000, 50% profit
					new AccountAction(new Time(6), new Trade(new MarketName("2"), SELL, 10, 100, 4))
				));
		Account account = new Account(0);
		AccountAction.performActionsForTime(new Time(0), actions, account);
		AccountAction.performActionsForTime(new Time(1), actions, account);
		AccountAction.performActionsForTime(new Time(2), actions, account);
		AccountAction.performActionsForTime(new Time(2), actions, account);
		assertEquals(1000, account.getPositionValue(new MarketName("1")));
		AccountAction.performActionsForTime(new Time(3), actions, account);
		AccountAction.performActionsForTime(new Time(4), actions, account);
		AccountAction.performActionsForTime(new Time(5), actions, account);
		AccountAction.performActionsForTime(new Time(6), actions, account);
		AccountAction.performActionsForTime(new Time(7), actions, account);
		assertEquals(10981, account.getCash());
		AccountHistory history = account.getAccountHistory();
		assertEquals(19, history.getPaidCommissions());
		assertEquals(2, history.countProfitableMarkets());
		assertEquals(1, history.countUnprofitableMarkets());
		assertEquals("1 100 %", history.biggestWin());
		assertEquals("3 -50 %", history.biggestLoss());
		assertEquals(10000, history.getCashPayments());
		TradeStats stats = history.getStats();
		assertEquals(66.6, stats.accuracy(), 0.1);
		assertEquals(75.0, stats.averageWinPercent(), 0.1);
		assertEquals(-50.0, stats.averageLossPercent(), 0.1);
		assertEquals(3.0, stats.profitFactor(), 0.1);
		assertEquals(0.6, stats.expectancy(), 0.1);
		assertEquals(3, stats.trades());
	}

}