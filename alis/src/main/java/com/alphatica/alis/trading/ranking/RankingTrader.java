package com.alphatica.alis.trading.ranking;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.MarketScore;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Direction;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.order.OrderSize;
import com.alphatica.alis.trading.strategy.Strategy;

import java.util.List;

import static com.alphatica.alis.tools.java.CollectionsTools.arrayList;

public class RankingTrader extends Strategy {

	public static final int MAX_PERCENT = 100;

	private final RankingProvider rankingProvider;
	private final int maxMarkets;
	private final int offset;

	public RankingTrader(RankingProvider rankingProvider, int maxMarkets, int offset) {
		if (maxMarkets > 100 || maxMarkets < 1) {
			throw new IllegalArgumentException("maxMarkets must be between [1, 100]");
		}
		this.rankingProvider = rankingProvider;
		this.maxMarkets = maxMarkets;
		this.offset = offset;
	}

	@Override
	public List<Order> afterClose(TimeMarketDataSet data, Account account) {
		List<Order> orders = arrayList();
		var ranking = rankingProvider.getRanking(data);
		for(var position: account.getPositions().entrySet()) {
			if (!marketInRanking(position.getKey(), ranking)) {
				orders.add(new Order(position.getKey(), Direction.SELL, OrderSize.PERCENTAGE, MAX_PERCENT, 1.0));
			}
		}
		for(int i = offset; i < maxMarkets + offset && i < ranking.size(); i++) {
			var toBuy = ranking.get(i);
			if (account.getPosition(toBuy.market()) == null) {
				orders.add(new Order(toBuy.market(), Direction.BUY, OrderSize.PERCENTAGE, MAX_PERCENT / maxMarkets, toBuy.value()));
			}
		}
		return orders;
	}

	private boolean marketInRanking(MarketName name, List<MarketScore> ranking) {
		for(int i = offset; i < maxMarkets + offset && i < ranking.size(); i++) {
			if (ranking.get(i).market().equals(name)) {
				return true;
			}
		}
		return false;
	}
}
