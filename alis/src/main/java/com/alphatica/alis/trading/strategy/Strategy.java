package com.alphatica.alis.trading.strategy;

import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Order;
import com.alphatica.alis.trading.strategy.params.Param;

import java.util.ArrayList;
import java.util.List;

public abstract class Strategy {
	protected List<Param> params = new ArrayList<>();

	public List<Param> getParams() {
		return params;
	}

	public void paramsChanged() {

	}

	public abstract List<Order> afterClose(TimeMarketDataSet data, Account account);

	public void finished(Account account) {
	}

}
