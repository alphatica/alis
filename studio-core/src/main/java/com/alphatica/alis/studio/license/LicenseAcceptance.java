package com.alphatica.alis.studio.license;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static com.alphatica.alis.studio.Constants.STUDIO_BUILD;
import static com.alphatica.alis.studio.Constants.STUDIO_WORK_DIR;

public final class LicenseAcceptance {

	private LicenseAcceptance() {
	}

	public static boolean isAccepted() throws IOException {
		File file = acceptanceFile();
		if (!file.exists() || !file.isFile()) {
			return false;
		}
		List<String> content = FileUtils.readLines(file, Charset.defaultCharset());
		return !content.isEmpty() && content.getFirst().equals(STUDIO_BUILD);
	}

	public static void accept() throws IOException {
		FileUtils.writeStringToFile(acceptanceFile(), STUDIO_BUILD, Charset.defaultCharset());
	}

	private static File acceptanceFile() {
		return new File(System.getProperty("user.home") + File.separator + STUDIO_WORK_DIR + File.separator + "AlisStudioAcceptance.txt");
	}
}
