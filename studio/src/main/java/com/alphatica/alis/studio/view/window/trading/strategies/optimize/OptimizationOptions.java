package com.alphatica.alis.studio.view.window.trading.strategies.optimize;

import com.alphatica.alis.studio.view.tools.components.SmartComboBox;
import com.alphatica.alis.trading.account.scorer.AccountScorer;
import com.alphatica.alis.trading.account.scorer.Expectancy;
import com.alphatica.alis.trading.account.scorer.NavAdjustedForMaxDD;
import com.alphatica.alis.trading.account.scorer.NetAssetValue;
import com.alphatica.alis.trading.account.scorer.ProfitableMarkets;
import com.alphatica.alis.trading.optimizer.ParametersSelection;
import com.alphatica.alis.trading.optimizer.ResultVerifier;

final class OptimizationOptions {

	private OptimizationOptions() {
	}

	static void configureResultVerifiers(SmartComboBox<ResultVerifier> comboBox) {
		comboBox.addOption("None", () -> ResultVerifier.NONE);
		comboBox.addOption("Remove markets", () -> ResultVerifier.REMOVE_MARKETS);
		comboBox.addOption("Remove orders", () -> ResultVerifier.REMOVE_ORDERS);
		comboBox.addOption("Fuzzy start time", () -> ResultVerifier.FUZZY_START_TIME);
	}

	static void configureParametersSelections(SmartComboBox<ParametersSelection> comboBox) {
		for (ParametersSelection selection : ParametersSelection.values()) {
			comboBox.addOption(selection.getText(), () -> selection);
		}
	}

	static void configureScorers(SmartComboBox<AccountScorer> comboBox) {
		comboBox.addOption("Net asset value", NetAssetValue::new);
		comboBox.addOption("Trade expectancy", Expectancy::new);
		comboBox.addOption("NAV / max drawdown", NavAdjustedForMaxDD::new);
		comboBox.addOption("Max profitable markets", ProfitableMarkets::new);
	}
}
