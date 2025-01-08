package com.alphatica.alis.trading.account;

import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.trading.account.actions.AccountAction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;

public class AccountHistory {
	private final Map<MarketName, List<PositionResult>> results;
	private final List<PositionPricesRecord> pricesRecords;
	private final List<AccountAction> accountActions;
	private double paidCommissions;
	private double cashPaymentsBalance;

	public AccountHistory(double cash) {
		this.results = HashMap.newHashMap(1024);
		cashPaymentsBalance = cash;
		pricesRecords = new ArrayList<>(1024);
		accountActions = new ArrayList<>(1024);
	}

	public void add(MarketName marketName, PositionResult result) {
		results.computeIfAbsent(marketName, k -> new ArrayList<>()).add(result);
	}

	public void addCommission(double commission) {
		paidCommissions += commission;
	}

	public double getPaidCommissions() {
		return paidCommissions;
	}

	public TradeStats getStats() {
		return calcStats(results.values().stream().flatMap(Collection::stream).toList());
	}

	public Map<MarketName, TradeStats> getAllStats() {
		return results.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> calcStats(entry.getValue())));
	}

	public long countProfitableMarkets() {
		Map<MarketName, TradeStats> allStats = getAllStats();
		return allStats.values().stream().map(s -> s.profitPerTrade() > 0).filter(v -> v).count();
	}

	public long countUnprofitableMarkets() {
		Map<MarketName, TradeStats> allStats = getAllStats();
		return allStats.values().stream().map(s -> s.profitPerTrade() < 0).filter(v -> v).count();
	}

	public String biggestWin() {
		double biggestPercent = 0;
		MarketName market = null;

		for (Map.Entry<MarketName, List<PositionResult>> entry : results.entrySet()) {
			for (PositionResult result : entry.getValue()) {
				if (result.profitPercent() > biggestPercent) {
					biggestPercent = result.profitPercent();
					market = entry.getKey();
				}
			}
		}
		return format("%s %.0f %%", market, biggestPercent);
	}

	public String biggestLoss() {
		double biggestPercent = 0;
		MarketName market = null;

		for (Map.Entry<MarketName, List<PositionResult>> entry : results.entrySet()) {
			for (PositionResult result : entry.getValue()) {
				if (result.profitPercent() < biggestPercent) {
					biggestPercent = result.profitPercent();
					market = entry.getKey();
				}
			}
		}
		return format("%s %.0f %%", market, biggestPercent);
	}

	public void recordCashPayment(double value) {
		cashPaymentsBalance += value;
	}

	public double getCashPayments() {
		return cashPaymentsBalance;
	}

	public List<PositionPricesRecord> getPricesRecords() {
		return new ArrayList<>(pricesRecords);
	}

	public void addRecords(List<PositionPricesRecord> positionPricesRecords) {
		pricesRecords.addAll(positionPricesRecords);
	}

	public void addAction(AccountAction accountAction) {
		accountActions.add(accountAction);
	}

	public List<AccountAction> getActions() {
		return new ArrayList<>(accountActions);
	}

	private TradeStats calcStats(List<PositionResult> list) {
		double sumWinPercent = 0;
		int winCount = 0;
		double sumLossPercent = 0;
		int lossCount = 0;
		for (PositionResult result : list) {
			if (result.profitValue() > 0) {
				winCount++;
				sumWinPercent += result.profitPercent();
			}
			if (result.profitValue() < 0) {
				lossCount++;
				sumLossPercent += result.profitPercent();
			}
		}
		int trades = list.size();
		double accuracy;
		if (lossCount > 0) {
			accuracy = ((double) winCount / (double) (winCount + lossCount)) * 100.0;
		} else {
			accuracy = 100.0;
		}
		double averageWinPercent = 0;
		if (winCount > 0) {
			averageWinPercent = sumWinPercent / winCount;
		}
		double averageLossPercent = 0;
		if (lossCount > 0) {
			averageLossPercent = sumLossPercent / lossCount;
		}

		double profitFactor = Double.NaN;
		double overallProfitPercent = sumWinPercent + sumLossPercent;
		if (sumLossPercent < 0.0) {
			profitFactor = sumWinPercent / -sumLossPercent;
		}
		double expectancy = 0;
		if (winCount + lossCount > 0 && averageLossPercent < 0) {
			double winProbability = (double) winCount / trades;
			expectancy = ((1 + averageWinPercent / -averageLossPercent) * winProbability) - 1;
		}
		return new TradeStats(overallProfitPercent / trades, accuracy, averageWinPercent, averageLossPercent, profitFactor, expectancy, trades);
	}
}
