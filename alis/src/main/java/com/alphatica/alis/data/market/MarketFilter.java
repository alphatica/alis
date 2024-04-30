package com.alphatica.alis.data.market;

import java.util.function.Predicate;

public class MarketFilter {
    public static final Predicate<Market> STOCKS = market -> market.getType() == MarketType.STOCK;
    public static final Predicate<Market> ALL = market -> true;

    private MarketFilter() {
    }
}
