package com.alphatica.alis.studio;

import com.alphatica.alis.studio.view.tools.ErrorDialog;
import com.alphatica.alis.studio.view.window.UserAgreementAcceptance;

import javax.swing.JTextArea;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

public class LicenseJTextField {

	public static JTextArea createTextAreaWithLicense() {
		JTextArea textArea = new JTextArea();
		try {
			String license = loadLicenseText();
			textArea.setText(license);
		} catch (IOException ex) {
			ErrorDialog.showError("Unable to load license text", ex.toString(), ex);
		}

		textArea.setEditable(false);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		textArea.setCaretPosition(0);
		return textArea;
	}

	private static String loadLicenseText() throws IOException {
		try (InputStream inputStream = UserAgreementAcceptance.class.getResourceAsStream("/LICENSE.txt")) {
			if (inputStream != null) {
				return new String(inputStream.readAllBytes(), Charset.defaultCharset());
			}
		}
		throw new IOException("Unable to load license text");
	}

    private LicenseJTextField() {
    }
}
