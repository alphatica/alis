package com.alphatica.alis.studio;

import com.alphatica.alis.studio.view.window.UserAgreementAcceptance;
import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;

public class StudioStart {
	public static void main(String[] args) {
		setupGUI();
	}

	private static void setupGUI() {
		FlatDarculaLaf.setup();

		UIManager.put("Table.alternateRowColor", true);
		UIManager.put("Table.alternateRowColor", new Color(60, 63, 65));

		SwingUtilities.invokeLater(UserAgreementAcceptance::checkAndStart);
	}

}