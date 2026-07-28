package com.alphatica.alis.trading.datamining.betterexits;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.Deposit;
import com.alphatica.alis.trading.account.actions.Trade;
import com.alphatica.alis.trading.account.actions.Withdrawal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.alphatica.alis.data.layer.Layer.OPEN;
import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;

final class SimulatedAccount {

	private final double commissionRate;
	private final Set<SellInfo> pendingSells = new HashSet<>();
	private final Account account = new Account(0);
	private List<AccountAction> actions;
	private double extraCash;

	SimulatedAccount(List<AccountAction> actions, double commissionRate) {
		this.actions = new ArrayList<>(actions);
		this.commissionRate = commissionRate;
	}

	Account account() {
		return account;
	}

	boolean hasPendingExit(MarketName market) {
		return pendingSells.stream().anyMatch(sell -> sell.marketName().equals(market));
	}

	void executePendingSells(Time time, MarketData marketData) throws AccountActionException {
		Iterator<SellInfo> iterator = pendingSells.iterator();
		while (iterator.hasNext()) {
			SellInfo info = iterator.next();
			TimeMarketData marketNow = marketData.getMarket(info.marketName()).getAt(time);
			if (marketNow == null) {
				continue;
			}
			double price = marketNow.getData(OPEN, 0);
			double commission = price * info.quantity() * commissionRate;
			new Trade(info.marketName(), SELL, price, info.quantity(), commission)
					.doOnAccount(time, account);
			iterator.remove();
		}
	}

	Set<MarketName> performActionsForTime(Time time) throws AccountActionException {
		Set<MarketName> closedMarkets = new HashSet<>();
		while (!actions.isEmpty() && !actions.getFirst().time().isAfter(time)) {
			AccountAction accountAction = actions.removeFirst();
			ensureCashForBuy(time, accountAction);
			accountAction.actionType().doOnAccount(time, account);
			recordClosedPosition(accountAction, closedMarkets);
		}
		return closedMarkets;
	}

	void reduceExtraCash(Time time) throws AccountActionException {
		if (account.getCash() > 0 && extraCash > 0) {
			double reduceBy = Math.min(extraCash, account.getCash());
			extraCash -= reduceBy;
			new Withdrawal(reduceBy).doOnAccount(time, account);
		}
	}

	void scheduleExit(MarketName market, int quantity) {
		actions = deleteFollowingSellActions(actions, market, quantity);
		pendingSells.add(new SellInfo(market, quantity));
	}

	Optional<Account> finish() throws AccountActionException {
		account.close(commissionRate);
		if (extraCash == 0) {
			return Optional.of(account);
		}
		if (extraCash > 0 && extraCash < account.getCash()) {
			new Withdrawal(extraCash).doOnAccount(null, account);
			extraCash = 0;
			return Optional.of(account);
		}
		return Optional.empty();
	}

	private void ensureCashForBuy(Time time, AccountAction accountAction) throws AccountActionException {
		if (accountAction.actionType() instanceof Trade trade && trade.direction() == BUY) {
			double value = trade.quantity() * trade.price() + trade.commission();
			if (value > account.getCash()) {
				double missing = 1 + value - account.getCash();
				extraCash += missing;
				new Deposit(missing).doOnAccount(time, account);
			}
		}
	}

	private void recordClosedPosition(AccountAction accountAction, Set<MarketName> closedMarkets) {
		if (accountAction.actionType() instanceof Trade trade
				&& trade.direction() == SELL
				&& account.getPosition(trade.marketName()) == null) {
			closedMarkets.add(trade.marketName());
		}
	}

	private static List<AccountAction> deleteFollowingSellActions(
			List<AccountAction> actions,
			MarketName market,
			int quantity) {
		List<AccountAction> newActions = new ArrayList<>(actions.size());
		int remainingQuantity = quantity;
		while (!actions.isEmpty()) {
			AccountAction action = actions.removeFirst();
			if (isMatchingSell(action, market, remainingQuantity)) {
				Trade trade = (Trade) action.actionType();
				int toReduce = Math.min(remainingQuantity, trade.quantity());
				remainingQuantity -= toReduce;
				addRemainingSell(newActions, action, trade, market, toReduce);
				continue;
			}
			newActions.add(action);
		}
		return newActions;
	}

	private static boolean isMatchingSell(AccountAction action, MarketName market, int remainingQuantity) {
		return remainingQuantity > 0
				&& action.actionType() instanceof Trade trade
				&& trade.direction() == SELL
				&& trade.marketName().equals(market);
	}

	private static void addRemainingSell(
			List<AccountAction> actions,
			AccountAction originalAction,
			Trade originalTrade,
			MarketName market,
			int reducedQuantity) {
		int leftQuantity = originalTrade.quantity() - reducedQuantity;
		if (leftQuantity > 0) {
			double commission = (leftQuantity / (double) originalTrade.quantity())
					* originalTrade.commission();
			actions.add(new AccountAction(
					originalAction.time(),
					new Trade(market, SELL, originalTrade.price(), leftQuantity, commission)));
		}
	}
}
