package com.alphatica.alis.studio.logic.trading.walkforward;

import com.alphatica.alis.data.market.MarketAttributes;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.optimizer.Optimizer;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.Strategy;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * One strategy whose parameters change on given days: from the first session on or after a day, its decisions use
 * that day's parameters. The strategy is the same object all along, so its open positions and its state (e.g. counters)
 * go on with the new parameters; they only decide which positions to open and when to close the open ones.
 */
public final class WalkForwardStrategy extends Strategy {
	private final Strategy strategy;
	private final NavigableMap<Time, Map<String, Object>> schedule;
	private Time applied;

	/**
	 * @param schedule parameters by the first day they are used; the first day is on or before the first session
	 */
	public WalkForwardStrategy(Strategy strategy, Map<Time, Map<String, Object>> schedule) {
		if (schedule.isEmpty()) {
			throw new IllegalArgumentException("No parameters to trade with");
		}
		this.strategy = strategy;
		this.schedule = new TreeMap<>(schedule);
	}

	@Override
	public List<Order> afterClose(TimeMarketDataSet data, Account account) {
		Map.Entry<Time, Map<String, Object>> current = schedule.floorEntry(data.getTime());
		if (current == null) {
			throw new IllegalStateException("No parameters for " + data.getTime());
		}
		if (!current.getKey().equals(applied)) {
			try {
				Optimizer.copyParameters(current.getValue(), strategy);
			} catch (IllegalAccessException e) {
				throw new IllegalStateException("Unable to set parameters of " + strategy.getClass().getSimpleName(), e);
			}
			applied = current.getKey();
		}
		return strategy.afterClose(data, account);
	}

	@Override
	public void finished(Account account) {
		strategy.finished(account);
	}

	@Override
	public Map<MarketName, MarketAttributes> getSummaryTable() {
		return strategy.getSummaryTable();
	}

	@Override
	public Map<String, Double> getCustomStats() {
		return strategy.getCustomStats();
	}
}
