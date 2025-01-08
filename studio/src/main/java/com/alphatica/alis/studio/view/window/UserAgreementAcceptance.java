package com.alphatica.alis.studio.view.window;

import com.alphatica.alis.studio.view.MainFrame;
import com.alphatica.alis.studio.view.tools.ErrorDialog;
import org.apache.commons.io.FileUtils;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import static com.alphatica.alis.studio.Constants.STUDIO_BUILD;
import static com.alphatica.alis.studio.Constants.STUDIO_NAME;
import static com.alphatica.alis.studio.Constants.STUDIO_WORK_DIR;
import static com.alphatica.alis.studio.LicenseJTextField.createTextAreaWithLicense;

public class UserAgreementAcceptance {

	private UserAgreementAcceptance() {
	}

	public static void checkAndStart() {
		if (isAlreadyAccepted()) {
			startMainApp();
		} else {
			showAcceptanceDialog();
		}
	}

	private static void startMainApp() {
		MainFrame mainFrame = new MainFrame();
		mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		ErrorDialog.setMainFrame(mainFrame);
		mainFrame.init();
	}

	private static boolean isAlreadyAccepted() {
		try {
			File file = new File(getAcceptanceFilePath());
			if (!file.exists() || !file.isFile()) {
				return false;
			}
			List<String> content = FileUtils.readLines(file, Charset.defaultCharset());
			if (content.isEmpty()) {
				return false;
			}
			return verifyAcceptanceForCurrentVersion(content);
		} catch (IOException ex) {
			ErrorDialog.showError("Unable to read ULA acceptance", ex.toString(), ex);
		}
		return false;
	}

	private static boolean verifyAcceptanceForCurrentVersion(List<String> content) {
		return content.getFirst().equals(STUDIO_BUILD);
	}

	private static void showAcceptanceDialog() {
		JFrame frame = initFrame();

		JTextArea textAreaWithLicense = createTextAreaWithLicense();
		frame.add(new JScrollPane(textAreaWithLicense), BorderLayout.CENTER);

		JPanel buttonPanel = createButtonsPanel();

		JButton accept = createAcceptButton(frame);
		buttonPanel.add(accept);

		JButton decline = createDeclineButton(frame);
		buttonPanel.add(decline);

		frame.add(buttonPanel, BorderLayout.SOUTH);
		frame.setVisible(true);
	}

	private static JPanel createButtonsPanel() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		return buttonPanel;
	}

	private static JButton createDeclineButton(JFrame frame) {
		JButton decline = new JButton("✘ Decline");
		decline.addActionListener(a -> frame.dispose());
		decline.setForeground(Color.RED);
		return decline;
	}

	private static JButton createAcceptButton(JFrame frame) {
		JButton accept = new JButton("✔ Accept");
		accept.addActionListener(a -> {
			frame.dispose();
			if (saveAcceptance(frame)) {
				startMainApp();
			} else {
				System.exit(0);
			}
		});
		accept.setForeground(Color.GREEN);
		return accept;
	}

	private static JFrame initFrame() {
		JFrame frame = new JFrame(STUDIO_NAME);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setSize(900, 900);
		frame.setLayout(new BorderLayout());
		return frame;
	}



	private static boolean saveAcceptance(JFrame frame) {
		try {
			String path = getAcceptanceFilePath();
			FileUtils.writeStringToFile(new File(path), STUDIO_BUILD, Charset.defaultCharset());
			return true;
		} catch (IOException ex) {
			ErrorDialog.setMainFrame(frame);
			ErrorDialog.showError("Unable to save ULA acceptance", ex.toString(), ex);
			return false;
		}
	}

	private static String getAcceptanceFilePath() {
		return System.getProperty("user.home") + File.separator + STUDIO_WORK_DIR + File.separator + "AlisStudioAcceptance.txt";
	}
}
