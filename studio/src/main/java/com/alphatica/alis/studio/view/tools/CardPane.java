package com.alphatica.alis.studio.view.tools;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.CardLayout;

public class CardPane {
	private final CardLayout cardLayout;
	private final JPanel mainPane;

	public CardPane() {
		cardLayout = new CardLayout();
		mainPane = new JPanel(cardLayout);
		mainPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	}

	public void showOnAction(JMenuItem menuItem, String name, JPanel panel) {
		mainPane.add(panel, name);
		menuItem.addActionListener(e -> cardLayout.show(mainPane, name));
	}

	public void setMainJFrame(JFrame frame) {
		frame.add(mainPane, BorderLayout.CENTER);
	}
}
