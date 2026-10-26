package com.alphatica.alis.trading.strategy.random;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.Position;
import com.alphatica.alis.trading.order.Direction;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.order.OrderSize;
import com.alphatica.alis.trading.strategy.Strategy;
import com.alphatica.alis.trading.optimizer.params.DoubleParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static com.alphatica.alis.data.time.TimeMarketDataFilters.STOCKS;

/**
 * Buys stocks at random: every session, each stock without a position is bought with {@code tradeProbability}.
 * <p>
 * Exit: by default a position is sold with the same probability every session. With {@link #setHoldingLengths} every
 * position gets a holding length drawn from the given list (e.g. trade lengths of another strategy) and is sold once
 * it has been held that many sessions.
 */
public class RandomStrategy extends Strategy {
	private static final double DEFAULT_POSITION_SIZE_PERCENT = 5;

	@DoubleParam(start = 0.0, step = 0.0001, end = 0.5)
	private double tradeProbability = 0.005;

	private double positionSizePercent = DEFAULT_POSITION_SIZE_PERCENT;
	private int[] holdingLengths;
	private final Map<MarketName, Integer> plannedLengths = new HashMap<>();

	public void setTradeProbability(double tradeProbability) {
		this.tradeProbability = tradeProbability;
	}

	public void setPositionSizePercent(double positionSizePercent) {
		this.positionSizePercent = positionSizePercent;
	}

	/**
	 * @param holdingLengths sessions to hold a position, one is drawn for every position; empty or null for random exits
	 */
	public void setHoldingLengths(int[] holdingLengths) {
		this.holdingLengths = holdingLengths == null || holdingLengths.length == 0 ? null : holdingLengths.clone();
	}

	@Override
	public List<Order> afterClose(TimeMarketDataSet timeMarketDataSet, Account account) {
		List<Order> orders = new ArrayList<>();
		ThreadLocalRandom random = ThreadLocalRandom.current();
		plannedLengths.keySet().retainAll(account.getPositions().keySet());
		timeMarketDataSet.listUpToDateMarkets(STOCKS).forEach(market -> {
			MarketName name = market.getMarketName();
			Position position = account.getPosition(name);
			if (position == null) {
				if (random.nextDouble() < tradeProbability) {
					orders.add(new Order(name, Direction.BUY, OrderSize.PERCENTAGE, positionSizePercent, 1));
				}
			} else if (shouldSell(name, position, random)) {
				orders.add(new Order(name, Direction.SELL, OrderSize.PERCENTAGE, 100, 1));
			}
		});
		return orders;
	}

	private boolean shouldSell(MarketName name, Position position, ThreadLocalRandom random) {
		if (holdingLengths == null) {
			return random.nextDouble() < tradeProbability;
		}
		int length = plannedLengths.computeIfAbsent(name, ignored -> holdingLengths[random.nextInt(holdingLengths.length)]);
		return position.getTradeLength() >= length;
	}
}
