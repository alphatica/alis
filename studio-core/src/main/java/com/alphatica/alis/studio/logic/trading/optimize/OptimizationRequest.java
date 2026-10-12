package com.alphatica.alis.studio.logic.trading.optimize;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.studio.logic.trading.backtest.BacktestSettings;
import com.alphatica.alis.trading.account.scorer.AccountScorer;
import com.alphatica.alis.trading.optimizer.OptimizerException;
import com.alphatica.alis.trading.optimizer.ParametersSelection;
import com.alphatica.alis.trading.optimizer.ResultVerifier;
import com.alphatica.alis.trading.optimizer.StrategyOptimizer;
import com.alphatica.alis.trading.strategy.StrategyExecutor;

import java.util.function.Supplier;

public record OptimizationRequest(BacktestSettings settings, MarketData marketData, Supplier<AccountScorer> scorerFactory,
		ResultVerifier resultVerifier, ParametersSelection parametersSelection, long maxCounter) {

	public StrategyOptimizer createOptimizer() throws OptimizerException {
		Supplier<StrategyExecutor> executorFactory = () -> new StrategyExecutor().withInitialCash(settings.initialCapital())
				.withCommissionRate(settings.commissionRate())
				.withTimeRange(settings.timeStart(), settings.timeEnd())
				.useCachedMarketData();
		return new StrategyOptimizer(settings.strategyFactory(), marketData, executorFactory, scorerFactory, resultVerifier,
				parametersSelection, maxCounter);
	}
}
