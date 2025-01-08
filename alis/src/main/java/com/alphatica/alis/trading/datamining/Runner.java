package com.alphatica.alis.trading.datamining;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.data.market.MarketName;
import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.data.time.TimeMarketData;
import com.alphatica.alis.data.time.TimeMarketDataSet;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.Position;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.Deposit;
import com.alphatica.alis.trading.account.actions.Trade;
import com.alphatica.alis.trading.account.actions.Withdrawal;
import com.alphatica.alis.trading.datamining.betterexits.BetterExitFinder;
import com.alphatica.alis.trading.account.scorer.AccountScorer;
import com.alphatica.alis.trading.datamining.betterexits.ExitFinderResult;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.alphatica.alis.data.layer.Layer.OPEN;
import static com.alphatica.alis.trading.order.Direction.BUY;
import static com.alphatica.alis.trading.order.Direction.SELL;
import static java.util.Optional.empty;
import static java.util.Optional.of;

public class Runner {
    double commissionRate = 0.01;
    double extraCash = 0.0;
    int trades = 0;

    public void run(MarketData marketData, List<AccountAction> actions, List<Supplier<BetterExitFinder>> exitFinderSuppliers, Supplier<AccountScorer> scorerSupplier, Consumer<ExitFinderResult> resultCallback) throws AccountActionException {
        BetterExitFinder betterExitFinder = exitFinderSuppliers.get(ThreadLocalRandom.current().nextInt(exitFinderSuppliers.size())).get();
        runFinder(marketData, copy(actions), betterExitFinder).ifPresent(account -> scoreAccount(account, scorerSupplier.get(), resultCallback, betterExitFinder));
    }

    private void scoreAccount(Account account, AccountScorer accountScorer, Consumer<ExitFinderResult> resultCallback, BetterExitFinder betterExitFinder) {
        double score = accountScorer.score(account, new HashMap<>());
        resultCallback.accept(new ExitFinderResult(account, score, trades, betterExitFinder.name(), betterExitFinder.description()));
    }

    private Optional<Account> runFinder(MarketData marketData, List<AccountAction> originalActions, BetterExitFinder betterExitFinder) throws AccountActionException {
        List<Time> times = getTimes(marketData, originalActions);
        MarketStateSet marketStateSet = new MarketStateSet();
        Set<SellInfo> pendingSells = new HashSet<>();
        List<AccountAction> actions = copy(originalActions);
        Account account = new Account(0);
        for (Time time : times) {
            executePendingSells(time, marketData, pendingSells, account);
            performActionsForTime(time, actions, account);
            reduceExtraCash(time, account);
            TimeMarketDataSet timeMarketDataSet = TimeMarketDataSet.build(time, marketData);
            account.updateLastKnown(timeMarketDataSet);
            for(Map.Entry<MarketName, Position> position: account.getPositions().entrySet()) {
                TimeMarketData timeMarketData = timeMarketDataSet.get(position.getKey());
                if (timeMarketData == null) {
                    continue;
                }
                if (betterExitFinder.shouldExit(account, timeMarketData, timeMarketDataSet, marketStateSet)) {
                    trades++;
                    actions = deleteFollowingSellActions(actions, position.getKey(), position.getValue().getQuantity());
                    SellInfo sell = new SellInfo(position.getKey(), position.getValue().getQuantity());
                    pendingSells.add(sell);
                    marketStateSet.delete(position.getKey());
                }
            }
        }
        return finishActions(account);
    }

    private Optional<Account> finishActions(Account account) throws AccountActionException {
        account.close(commissionRate);
        if (extraCash == 0) {
            return of(account);
        } else if (extraCash > 0 && extraCash < account.getCash()) {
            new Withdrawal(extraCash).doOnAccount(null, account);
            extraCash = 0;
            return of(account);
        } else {
            return empty();
        }
    }

    private void executePendingSells(Time time, MarketData marketData, Set<SellInfo> pendingSells, Account account) throws AccountActionException {
        Iterator<SellInfo> iterator = pendingSells.iterator();
        while(iterator.hasNext()) {
            SellInfo info = iterator.next();
            TimeMarketData marketNow = marketData.getMarket(info.marketName()).getAt(time);
            if (marketNow == null) {
                continue;
            }
            double price = marketNow.getData(OPEN, 0);
            double commission = price * info.quantity() * commissionRate;
            Trade trade = new Trade(info.marketName(), SELL, price, info.quantity(), commission);
            trade.doOnAccount(time, account);
            iterator.remove();
        }
    }

    private List<AccountAction> deleteFollowingSellActions(List<AccountAction> actions, MarketName market, int quantity) {
        List<AccountAction> newActions = new ArrayList<>(actions.size());
        int remainingQuantity = quantity;
        while (!actions.isEmpty()) {
            AccountAction action = actions.removeFirst();
            if (remainingQuantity > 0 && action.actionType() instanceof Trade trade && trade.direction() == SELL && trade.marketName()
                                                                                                                         .equals(market)) {
                int toReduce = Math.min(remainingQuantity, trade.quantity());
                remainingQuantity -= toReduce;
                int leftQuantity = trade.quantity() - toReduce;
                if (leftQuantity > 0) {
                    double newCommission = (leftQuantity / (double) trade.quantity()) * trade.commission();
                    newActions.add(new AccountAction(action.time(), new Trade(market, SELL, trade.price(), leftQuantity, newCommission)));
                }
                continue;
            }
            newActions.add(action);
        }
        return newActions;
    }

    private List<Time> getTimes(MarketData marketData, List<AccountAction> accountActions) {
        if (!accountActions.isEmpty()) {
            return marketData.getTimes().stream().filter(t -> !t.isBefore(accountActions.getFirst().time())).toList();
        } else {
            return Collections.emptyList();
        }
    }

    private List<AccountAction> copy(List<AccountAction> originalActions) {
        return new ArrayList<>(originalActions);
    }

    private void performActionsForTime(Time time, List<AccountAction> accountActions, Account account) throws AccountActionException {
        while (!accountActions.isEmpty() && !accountActions.getFirst().time().isAfter(time)) {
            AccountAction accountAction = accountActions.removeFirst();
            if (accountAction.actionType() instanceof Trade trade && trade.direction() == BUY) {
                double value = trade.quantity() * trade.price() + trade.commission();
                if (value > account.getCash()) {
                    double missing = 1 + value - account.getCash();
                    extraCash += missing;
                    Deposit deposit = new Deposit(missing);
                    deposit.doOnAccount(time, account);
                }
            }
            accountAction.actionType().doOnAccount(time, account);
        }
    }

    private void reduceExtraCash(Time time, Account account) throws AccountActionException {
        if (account.getCash() > 0 && extraCash > 0) {
            double reduceBy = Math.min(extraCash, account.getCash());
            extraCash -= reduceBy;
            Withdrawal withdrawal = new Withdrawal(reduceBy);
            withdrawal.doOnAccount(time, account);
        }
    }
}
