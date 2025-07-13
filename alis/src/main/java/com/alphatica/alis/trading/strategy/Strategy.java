package com.alphatica.alis.trading.strategy;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.tools.java.MarketAttributes;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.optimizer.Optimizable;
import com.alphatica.alis.trading.order.Order;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public abstract class Strategy implements Optimizable {

	public void paramsChanged() {
	}

	public abstract List<Order> afterClose(TimeMarketDataSet data, Account account);

	public void finished(Account account) {
	}

	public Map<MarketName, MarketAttributes> getSummaryTable() {
		return Collections.emptyMap();
	}

	public Map<String, Double> getCustomStats() {
		return Collections.emptyMap();
	}

}
