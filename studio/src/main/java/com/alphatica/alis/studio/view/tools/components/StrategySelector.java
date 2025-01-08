package com.alphatica.alis.studio.view.tools.components;

import com.alphatica.alis.studio.strategy.StrategyList;
import com.alphatica.alis.studio.view.tools.ErrorDialog;
import com.alphatica.alis.trading.strategy.Strategy;

public class StrategySelector extends SmartComboBox<Strategy> {

	public StrategySelector() {
		for (Class<? extends Strategy> strategy : StrategyList.strategies) {
			addOption(strategy.getSimpleName(), () -> getStrategy(strategy));
		}
	}

	private static Strategy getStrategy(Class<? extends Strategy> strategy) {
		try {
			return strategy.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			ErrorDialog.showError("Unable to create strategy", e.toString(), e);
		}
		return null;
	}
}
