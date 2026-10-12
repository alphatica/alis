package com.alphatica.alis.studio.logic.trading.signals;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.studio.logic.table.Column;
import com.alphatica.alis.studio.logic.trading.account.AccountProvider;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.alphatica.alis.trading.account.actions.AccountAction.performActionsForTime;

/**
 * Orders a strategy gives at every market session from a start time. Rows are newest first, by priority within a
 * session (see {@link #COLUMNS}).
 */
public final class SignalsProvider {

	public static final List<Column> COLUMNS = List.of(Column.date("Time"), Column.text("Symbol"), Column.text("Type"),
			Column.number("Priority", 2), Column.text("Size"));

	private SignalsProvider() {
	}

	/**
	 * Signals for the user's portfolio: its actions are replayed from the first one (or the last session without
	 * actions).
	 */
	public static List<Object[]> forPortfolio(Strategy strategy, MarketData marketData) throws IOException, AccountActionException {
		List<AccountAction> accountActions = AccountProvider.getAccountActions();
		Time startTime = accountActions.isEmpty() ? marketData.getTimes().getLast() : accountActions.getFirst().time();
		return signals(strategy, marketData, startTime, accountActions, new Account(0));
	}

	/**
	 * Signals for an account starting with 100 000 cash at {@code startTime}.
	 */
	public static List<Object[]> fromTime(Strategy strategy, MarketData marketData, Time startTime) throws AccountActionException {
		return signals(strategy, marketData, startTime, Collections.emptyList(), new Account(100_000));
	}

	private static List<Object[]> signals(Strategy strategy, MarketData marketData, Time startTime,
	                                      List<AccountAction> accountActions, Account account) throws AccountActionException {
		List<Object[]> rows = new ArrayList<>();
		for (Time time : marketData.getTimes()) {
			if (time.isBefore(startTime)) {
				continue;
			}
			TimeMarketDataSet data = marketData.snapshotAt(time);
			performActionsForTime(time, accountActions, account);
			account.updateLastKnown(data);
			List<Order> orders = strategy.afterClose(data, account);
			Collections.sort(orders);
			for (Order order : orders) {
				rows.add(new Object[]{time.time(), order.market().name(), order.direction().toString(), order.priority(),
						order.formatSize()});
			}
		}
		return rows.reversed();
	}
}
