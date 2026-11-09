package com.alphatica.alis.studio.logic.trading.walkforward;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.logic.trading.walkforward.WalkForward.OptimizedWindow;
import com.alphatica.alis.studio.logic.trading.walkforward.WalkForward.TestedWindow;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.Trade;

import java.util.ArrayList;
import java.util.List;

final class TestedWindows {
	private TestedWindows() {
	}

	static List<TestedWindow> of(List<OptimizedWindow> optimized, Account account) {
		List<Time> times = account.getAccountHistory().getSessions().times();
		double[] returns = account.getAccountHistory().getSessions().returns();
		List<AccountAction> actions = account.getAccountHistory().getActions();
		List<TestedWindow> result = new ArrayList<>(optimized.size());
		for (OptimizedWindow window : optimized) {
			Time from = window.window().testStart();
			Time to = window.window().testEnd();
			double growth = 1;
			for (int i = 0; i < times.size(); i++) {
				if (isWithin(times.get(i), from, to)) {
					growth *= 1 + returns[i];
				}
			}
			int transactions = 0;
			for (AccountAction action : actions) {
				if (action.actionType() instanceof Trade && isWithin(action.time(), from, to)) {
					transactions++;
				}
			}
			result.add(new TestedWindow(window, (growth - 1) * 100, transactions));
		}
		return result;
	}

	private static boolean isWithin(Time time, Time from, Time to) {
		return !time.isBefore(from) && !time.isAfter(to);
	}
}
