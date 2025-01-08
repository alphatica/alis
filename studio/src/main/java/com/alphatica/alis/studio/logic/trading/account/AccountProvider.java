package com.alphatica.alis.studio.logic.trading.account;

import com.alphatica.alis.studio.dao.AccountDao;
import com.alphatica.alis.studio.dao.DaoException;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;

import java.util.List;

import static com.alphatica.alis.studio.state.ChangeListeners.publish;
import static com.alphatica.alis.studio.state.StateChange.PORTFOLIO_CHANGED;
import static java.util.Comparator.comparing;

public class AccountProvider {

	private AccountProvider() {
	}

	public static List<AccountAction> getAccountActions() throws DaoException {
		return AccountDao.readActions();
	}

	public static void saveAction(AccountAction action) throws DaoException, AccountActionException {
		validateAction(action);
		AccountDao.saveAction(action);
		publish(PORTFOLIO_CHANGED);
	}

	public static void saveActions(List<AccountAction> accountActions) throws DaoException, AccountActionException {
		List<AccountAction> existingActions = getAccountActions();
		existingActions.addAll(accountActions);
		existingActions.sort(comparing(AccountAction::time));
		validateActions(existingActions);
		AccountDao.replaceActions(existingActions);
		publish(PORTFOLIO_CHANGED);
	}

	private static void validateAction(AccountAction action) throws DaoException, AccountActionException {
		List<AccountAction> actions = getAccountActions();
		if (!actions.isEmpty() && action.time().isBefore(actions.getLast().time())) {
			throw new AccountActionException("New action time is before last action's time");
		}
		actions.add(action);
		validateActions(actions);
	}

	private static void validateActions(List<AccountAction> actions) throws AccountActionException {
		Account account = new Account(0);
		for (AccountAction accountAction : actions) {
			accountAction.actionType().doOnAccount(accountAction.time(), account);
		}
	}
}
