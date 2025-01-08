package com.alphatica.alis.studio.tools;

import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionParser;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public class AccountActionCSVFacade {

	private AccountActionCSVFacade() {
	}

	public static List<AccountAction> readActions(File file) throws IOException {
		if (!file.exists()) {
			return new ArrayList<>();
		}
		List<AccountAction> actions = new ArrayList<>(1024);
		List<String> rows = FileUtils.readLines(file, Charset.defaultCharset());
		for (String row : rows) {
			AccountAction action = AccountActionParser.fromCsv(row);
			actions.add(action);
		}
		return actions;
	}

	public static void saveActions(List<AccountAction> actions, File file) throws IOException {
		List<String> lines = actions.stream().map(AccountAction::toCsv).toList();
		FileUtils.writeLines(file, lines, false);
	}

	public static void saveAction(AccountAction action, File file) throws IOException {
		String row = action.toCsv();
		FileUtils.writeStringToFile(file, format("%s%n", row), Charset.defaultCharset(), true);
	}

	public static void replaceActions(List<AccountAction> actions, File file) throws IOException {
		File tmpFile = saveToTmpFile(actions);
		Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}

	private static File saveToTmpFile(List<AccountAction> actions) throws IOException {
		Path tmpPath = Files.createTempFile("account", "csv");
		List<String> content = actions.stream().map(AccountAction::toCsv).toList();
		File tmpFile = tmpPath.toFile();
		FileUtils.writeLines(tmpFile, content);
		return tmpFile;
	}
}
