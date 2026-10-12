package com.alphatica.alis.studio.dao;

import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
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
		try (Reader reader = new InputStreamReader(Files.newInputStream(file.toPath()), Charset.defaultCharset())) {
			return readActions(reader);
		}
	}

	public static List<AccountAction> readActions(Reader reader) throws IOException {
		List<AccountAction> actions = new ArrayList<>(1024);
		List<String> rows = IOUtils.readLines(reader);
		for (String row : rows) {
			String content = stripComment(row);
			if (!content.isEmpty()) {
				actions.add(AccountActionParser.fromCsv(content));
			}
		}
		return actions;
	}

	// '#' starts a comment, either on its own line or after an action
	private static String stripComment(String row) {
		int commentStart = row.indexOf('#');
		String content = commentStart >= 0 ? row.substring(0, commentStart) : row;
		return content.trim();
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
