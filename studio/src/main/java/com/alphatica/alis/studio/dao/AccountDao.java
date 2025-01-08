package com.alphatica.alis.studio.dao;

import com.alphatica.alis.studio.tools.AccountActionCSVFacade;
import com.alphatica.alis.trading.account.actions.AccountAction;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static com.alphatica.alis.studio.Constants.STUDIO_WORK_DIR;

public class AccountDao {

	private static final String USER_DATA_DIR = System.getProperty("user.home") + File.separator + STUDIO_WORK_DIR;
	private static final String ACCOUNT_FILE_NAME = "account.csv";
	private static final File ACCOUNT_FILE = new File(USER_DATA_DIR + File.separator + ACCOUNT_FILE_NAME);

	private AccountDao() {
	}

	public static void saveAction(AccountAction action) throws DaoException {
		try {
			AccountActionCSVFacade.saveAction(action, ACCOUNT_FILE);
		} catch (IOException e) {
			throw new DaoException("Unable to save accounts file: " + e, e);
		}
	}

	public static List<AccountAction> readActions() throws DaoException {
		try {
			return AccountActionCSVFacade.readActions(ACCOUNT_FILE);
		} catch (Exception e) {
			throw new DaoException("Unable to read accounts file: " + e, e);
		}
	}

	public static void replaceActions(List<AccountAction> accountActions) throws DaoException {
		try {
			AccountActionCSVFacade.replaceActions(accountActions, ACCOUNT_FILE);
		} catch (Exception e) {
			throw new DaoException("Unable to read accounts file: " + e, e);
		}
	}
}
