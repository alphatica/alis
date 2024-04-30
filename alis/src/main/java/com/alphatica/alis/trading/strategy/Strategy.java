package com.alphatica.alis.trading.strategy;

import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.order.Order;

import java.util.List;

public interface Strategy {
    List<Order> afterClose(TimeMarketDataSet data, Account account);

    default void finished(Account account) {
    }
}
