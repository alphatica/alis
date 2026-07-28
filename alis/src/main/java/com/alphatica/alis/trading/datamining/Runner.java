package com.alphatica.alis.trading.datamining;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.scorer.AccountScorer;
import com.alphatica.alis.trading.datamining.betterexits.BetterExitFinder;
import com.alphatica.alis.trading.datamining.betterexits.BetterExitSimulation;
import com.alphatica.alis.trading.datamining.betterexits.BetterExitSimulationResult;
import com.alphatica.alis.trading.datamining.betterexits.ExitFinderResult;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Runner {

	private final double commissionRate;

	public Runner(double commissionRate) {
		if (!Double.isFinite(commissionRate) || commissionRate < 0 || commissionRate >= 1) {
			throw new IllegalArgumentException("commissionRate must be finite and in [0, 1)");
		}
		this.commissionRate = commissionRate;
	}

	public void run(
			MarketData marketData,
			List<AccountAction> actions,
			List<Supplier<BetterExitFinder>> exitFinderSuppliers,
			Supplier<AccountScorer> scorerSupplier,
			Consumer<ExitFinderResult> resultCallback) throws AccountActionException {
		BetterExitFinder betterExitFinder = selectFinder(exitFinderSuppliers);
		BetterExitSimulation simulation = new BetterExitSimulation(
				marketData, actions, betterExitFinder, commissionRate);
		simulation.run().ifPresent(result -> scoreAccount(
				result, scorerSupplier.get(), resultCallback, betterExitFinder));
	}

	private static BetterExitFinder selectFinder(List<Supplier<BetterExitFinder>> exitFinderSuppliers) {
		int index = ThreadLocalRandom.current().nextInt(exitFinderSuppliers.size());
		return exitFinderSuppliers.get(index).get();
	}

	private static void scoreAccount(
			BetterExitSimulationResult simulationResult,
			AccountScorer accountScorer,
			Consumer<ExitFinderResult> resultCallback,
			BetterExitFinder betterExitFinder) {
		Account account = simulationResult.account();
		double score = accountScorer.score(account, new HashMap<>());
		resultCallback.accept(new ExitFinderResult(
				account,
				score,
				simulationResult.trades(),
				betterExitFinder.name(),
				betterExitFinder.description()));
	}
}
