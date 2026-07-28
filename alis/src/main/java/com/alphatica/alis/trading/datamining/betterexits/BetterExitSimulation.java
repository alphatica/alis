package com.alphatica.alis.trading.datamining.betterexits;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.Position;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class BetterExitSimulation {

	private final MarketData marketData;
	private final BetterExitFinder betterExitFinder;
	private final MarketStateSet marketStateSet = new MarketStateSet();
	private final SimulatedAccount simulatedAccount;
	private final List<Time> times;
	private int trades;

	public BetterExitSimulation(
			MarketData marketData,
			List<AccountAction> actions,
			BetterExitFinder betterExitFinder,
			double commissionRate) {
		this.marketData = marketData;
		this.betterExitFinder = betterExitFinder;
		this.simulatedAccount = new SimulatedAccount(actions, commissionRate);
		this.times = simulationTimes(marketData, actions);
	}

	public Optional<BetterExitSimulationResult> run() throws AccountActionException {
		for (Time time : times) {
			simulatedAccount.executePendingSells(time, marketData);
			simulatedAccount.performActionsForTime(time);
			simulatedAccount.reduceExtraCash(time);
			TimeMarketDataSet currentData = marketData.snapshotAt(time);
			simulatedAccount.account().updateLastKnown(currentData);
			evaluateOpenPositions(currentData);
		}
		return simulatedAccount.finish()
				.map(account -> new BetterExitSimulationResult(account, trades));
	}

	private void evaluateOpenPositions(TimeMarketDataSet currentData) {
		Account account = simulatedAccount.account();
		for (Map.Entry<MarketName, Position> position : account.getPositions().entrySet()) {
			TimeMarketData marketDataAtTime = currentData.get(position.getKey());
			if (marketDataAtTime == null) {
				continue;
			}
			if (betterExitFinder.shouldExit(account, marketDataAtTime, currentData, marketStateSet)) {
				trades++;
				simulatedAccount.scheduleExit(position.getKey(), position.getValue().getQuantity());
				marketStateSet.delete(position.getKey());
			}
		}
	}

	private static List<Time> simulationTimes(MarketData marketData, List<AccountAction> actions) {
		if (actions.isEmpty()) {
			return Collections.emptyList();
		}
		Time firstActionTime = actions.getFirst().time();
		return marketData.getTimes().stream()
				.filter(time -> !time.isBefore(firstActionTime))
				.toList();
	}
}
