package com.alphatica.alis.trading.datamining;

import com.alphatica.alis.data.market.MarketData;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.scorer.AccountScorer;
import com.alphatica.alis.trading.datamining.betterexits.BetterExitFinder;
import com.alphatica.alis.trading.datamining.betterexits.DaysInPosition;
import com.alphatica.alis.trading.datamining.betterexits.ExitFinderResult;
import com.alphatica.alis.trading.datamining.betterexits.ExitIfSmallProfitAfter;
import com.alphatica.alis.trading.datamining.betterexits.SellStrong;
import com.alphatica.alis.trading.datamining.betterexits.SellWeak;
import com.alphatica.alis.trading.datamining.betterexits.Sma;
import com.alphatica.alis.trading.datamining.betterexits.TrailingStop;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;

public final class BetterExitsSearch {
	private static final List<Supplier<BetterExitFinder>> EXIT_FINDERS = List.of(
			Sma::generator,
			DaysInPosition::generator,
			TrailingStop::generator,
			SellWeak::generate,
			SellStrong::generate,
			ExitIfSmallProfitAfter::generate
	);

	private final AtomicBoolean isStopped = new AtomicBoolean(false);
	private final AtomicInteger iterationsDone = new AtomicInteger(0);
	private final MarketData marketData;
	private final List<AccountAction> accountActions;
	private final Supplier<AccountScorer> scorerFactory;
	private final double commissionRate;

	public BetterExitsSearch(MarketData marketData, List<AccountAction> accountActions, Supplier<AccountScorer> scorerFactory,
			double commissionRate) {
		this.marketData = marketData;
		this.accountActions = List.copyOf(accountActions);
		this.scorerFactory = scorerFactory;
		this.commissionRate = commissionRate;
	}

	/**
	 * Runs one task per processor and blocks until {@link #stop()} is called.
	 * The listener receives each result together with the number of iterations done so far.
	 */
	public void run(ObjIntConsumer<ExitFinderResult> resultListener) {
		int processors = Runtime.getRuntime().availableProcessors();
		CountDownLatch tasksFinished = new CountDownLatch(processors);
		for (int i = 0; i < processors; i++) {
			Thread.ofVirtual().start(() -> {
				try {
					runUntilStopped(resultListener);
				} finally {
					tasksFinished.countDown();
				}
			});
		}
		waitForFinish(tasksFinished);
	}

	public void stop() {
		isStopped.set(true);
	}

	private void runUntilStopped(ObjIntConsumer<ExitFinderResult> resultListener) {
		while (!isStopped.get()) {
			Runner runner = new Runner(commissionRate);
			try {
				runner.run(marketData, accountActions, EXIT_FINDERS, scorerFactory,
						result -> resultListener.accept(result, iterationsDone.incrementAndGet()));
			} catch (AccountActionException e) {
			}
		}
	}

	private void waitForFinish(CountDownLatch tasksFinished) {
		try {
			tasksFinished.await();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			stop();
		}
	}
}
