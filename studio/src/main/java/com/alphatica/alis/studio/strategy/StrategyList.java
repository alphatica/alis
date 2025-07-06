package com.alphatica.alis.studio.strategy;

import com.alphatica.alis.trading.strategy.Strategy;

import java.util.List;

public class StrategyList {

	public static List<Class<? extends Strategy>> strategies = List.of(
			Caretta.class,
			DonchianChannel.class,
			BuyATH.class,
			RandomStrategy.class);
}
