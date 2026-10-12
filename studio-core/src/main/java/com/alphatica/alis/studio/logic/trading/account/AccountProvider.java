package com.alphatica.alis.studio.logic.trading.account;

import com.alphatica.alis.studio.dao.AccountActionCSVFacade;
import com.alphatica.alis.trading.account.Account;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.alphatica.alis.studio.Constants.STUDIO_WORK_DIR;
import static java.util.Comparator.comparing;

/**
 * The user's account actions, kept in the account file in the work directory. Actions are validated before saving.
 */
public class AccountProvider {
	private static final File ACCOUNT_FILE = new File(System.getProperty("user.home") + File.separator + STUDIO_WORK_DIR
			+ File.separator + "account.csv");

	private AccountProvider() {
	}

	public static List<AccountAction> getAccountActions() throws IOException {
		try {
			return AccountActionCSVFacade.readActions(ACCOUNT_FILE);
		} catch (IllegalArgumentException | IllegalStateException e) {
			throw new IOException("Invalid account file " + ACCOUNT_FILE + ": " + e.getMessage(), e);
		}
	}

	public static void saveAction(AccountAction action) throws IOException, AccountActionException {
		List<AccountAction> actions = getAccountActions();
		if (!actions.isEmpty() && action.time().isBefore(actions.getLast().time())) {
			throw new AccountActionException("New action time is before last action's time");
		}
		actions.add(action);
		validateActions(actions);
		AccountActionCSVFacade.saveAction(action, ACCOUNT_FILE);
	}

	/**
	 * Adds the actions to the existing ones, sorted by time.
	 */
	public static void saveActions(List<AccountAction> accountActions) throws IOException, AccountActionException {
		List<AccountAction> existingActions = getAccountActions();
		existingActions.addAll(accountActions);
		existingActions.sort(comparing(AccountAction::time));
		validateActions(existingActions);
		AccountActionCSVFacade.replaceActions(existingActions, ACCOUNT_FILE);
	}

	private static void validateActions(List<AccountAction> actions) throws AccountActionException {
		Account account = new Account(0);
		for (AccountAction accountAction : actions) {
			accountAction.actionType().doOnAccount(accountAction.time(), account);
		}
	}
}
