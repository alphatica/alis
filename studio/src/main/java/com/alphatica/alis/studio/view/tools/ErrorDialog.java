package com.alphatica.alis.studio.view.tools;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import static com.alphatica.alis.studio.view.tools.ErrorReporter.reportError;
import static com.alphatica.alis.studio.view.tools.SwingHelper.runUiThread;

public class ErrorDialog {
	private static JFrame mainFrame;

	private ErrorDialog() {
	}

	public static void setMainFrame(JFrame frame) {
		mainFrame = frame;
	}

	public static void showError(String title, String message, Exception e) {
		runUiThread(() -> {
			Object[] options;
			if (e != null) {
				options = new Object[]{"Close", "Report"};
			} else {
				options = new Object[]{"Close"};
			}
			int choice = JOptionPane.showOptionDialog(
					mainFrame,
					message,
					title,
					JOptionPane.DEFAULT_OPTION,
					JOptionPane.ERROR_MESSAGE,
					null,
					options,
					options[0]
			);
			if (choice == 1) {
				reportError(message, e);
			}
		});
	}
}
