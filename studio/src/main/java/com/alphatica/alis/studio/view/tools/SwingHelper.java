package com.alphatica.alis.studio.view.tools;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.function.Consumer;

public class SwingHelper {
	private SwingHelper() {
	}

	public static JMenuItem createMenuActivatedPanel(String name, JPanel jPanel, CardPane mainCardPane) {
		JMenuItem item = new JMenuItem(name);
		mainCardPane.showOnAction(item, name, jPanel);
		return item;
	}

	public static JLabel createHtmlLinkLabel(String text, String uri) {
		// Create the hyperlink label
		JLabel hyperlinkLabel = new JLabel(String.format("<html><a href=''>%s</a></html>", text));
		hyperlinkLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));

		// Add mouse listener to open a browser
		hyperlinkLabel.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					Desktop.getDesktop().browse(new URI(uri));
				} catch (IOException | URISyntaxException ex) {
					ErrorDialog.showError("Unable to open browser", ex.toString(), ex);
				}
			}
		});
		return hyperlinkLabel;
	}

	public static void runOnAction(JButton button, Consumer<ActionEvent> consumer) {
		button.addActionListener(e -> new Thread(() -> consumer.accept(e)).start());
	}

	public static Runnable buildUiThread(Runnable runnable) {
		return () -> SwingUtilities.invokeLater(runnable);
	}

	public static void runUiThread(Runnable runnable) {
		SwingUtilities.invokeLater(runnable);
	}
}
