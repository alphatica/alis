package com.alphatica.alis.studio.license;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public final class LicenseText {

	private LicenseText() {
	}

	public static String read() throws IOException {
		try (InputStream inputStream = LicenseText.class.getResourceAsStream("/LICENSE.txt")) {
			if (inputStream != null) {
				return new String(inputStream.readAllBytes(), Charset.defaultCharset());
			}
		}
		throw new IOException("Unable to load license text");
	}
}
