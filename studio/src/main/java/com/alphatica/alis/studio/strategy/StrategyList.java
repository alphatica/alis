package com.alphatica.alis.studio.strategy;

import com.alphatica.alis.trading.strategy.Strategy;

import java.util.List;

public class StrategyList {

	public static final List<Class<? extends Strategy>> strategies = List.of(
			DonchianChannel.class,
			BuyATH.class,
			RandomStrategy.class);

    private StrategyList() {
    }
}
