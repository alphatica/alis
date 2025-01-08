package com.alphatica.alis.trading.strategy;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Order;

import java.util.List;

@FunctionalInterface
public interface BarExecutedConsumer {

	void execute(Time time, Account account, List<Order> orders);
}
