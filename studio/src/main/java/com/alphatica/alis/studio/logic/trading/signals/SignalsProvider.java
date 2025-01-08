package com.alphatica.alis.studio.logic.trading.signals;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.studio.dao.DaoException;
import com.alphatica.alis.studio.logic.trading.account.AccountProvider;
import com.alphatica.alis.studio.state.AppState;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.alphatica.alis.trading.account.actions.AccountAction.performActionsForTime;
import static java.lang.String.format;

public class SignalsProvider {

	private SignalsProvider() {
	}

	public static List<String[]> getOrdersTableData(Strategy strategy, boolean applyToPortfolio, Time startTime) throws DaoException,
			AccountActionException {
		List<String[]> ordersTableData;
		MarketData marketData = AppState.getMarketData();
		if (applyToPortfolio) {
			ordersTableData = getOrdersForPortfolio(strategy, marketData);
		} else {
			ordersTableData = getOrdersFromTime(strategy, startTime, marketData);
		}
		return ordersTableData;
	}

	private static List<String[]> getOrdersForPortfolio(Strategy strategy, MarketData marketData) throws DaoException, AccountActionException {
		List<AccountAction> accountActions = AccountProvider.getAccountActions();
		Time startTime = getStartTimeForAccountActions(accountActions, marketData);
		List<Time> times = filterTimes(startTime, marketData);
		return getOrdersForTimes(strategy, times, accountActions, new Account(0), marketData);
	}

	private static List<String[]> getOrdersFromTime(Strategy strategy, Time startTime, MarketData marketData) throws AccountActionException {
		List<Time> times = filterTimes(startTime, marketData);
		return getOrdersForTimes(strategy, times, Collections.emptyList(), new Account(100_000), marketData);
	}

	private static List<String[]> getOrdersForTimes(Strategy strategy, List<Time> times, List<AccountAction> accountActions, Account account, MarketData marketData) throws AccountActionException {
		List<String[]> ordersTableData = new ArrayList<>();
		for (Time time : times) {
			TimeMarketDataSet timeMarketDataSet = TimeMarketDataSet.build(time, marketData);
			performActionsForTime(time, accountActions, account);
			List<Order> orders = applyTimeData(account, timeMarketDataSet, strategy);
			fillSignalsList(time, orders, ordersTableData);
		}
		return ordersTableData;
	}

	private static Time getStartTimeForAccountActions(List<AccountAction> accountActions, MarketData marketData) {
		Time startTime;
		if (accountActions.isEmpty()) {
			startTime = marketData.getTimes().getLast();
		} else {
			startTime = accountActions.getFirst().time();
		}
		return startTime;
	}

	private static void fillSignalsList(Time time, List<Order> orders, List<String[]> ordersTableData) {
		Collections.sort(orders);
		for (Order order : orders) {
			ordersTableData.add(new String[]{time.toString(), order.market().name(), order.direction().toString(), format("%.2f", order.priority()),
					order.formatSize()});
		}
	}

	private static List<Order> applyTimeData(Account account, TimeMarketDataSet timeMarketDataSet, Strategy strategy) {
		account.updateLastKnown(timeMarketDataSet);
		List<Order> orders = strategy.afterClose(timeMarketDataSet, account);
		Collections.sort(orders);
		Collections.reverse(orders);
		return orders;
	}

	private static List<Time> filterTimes(Time startTime, MarketData marketData) {
		return marketData.getTimes().stream().filter(t -> !t.isBefore(startTime)).toList();
	}
}
