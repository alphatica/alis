package com.alphatica.alis.studio.view;

import com.alphatica.alis.studio.LicenseJTextField;
import com.alphatica.alis.studio.view.tools.CardPane;
import com.alphatica.alis.studio.view.tools.ErrorDialog;
import com.alphatica.alis.studio.view.window.analysis.FindBetterExitsPanel;
import com.alphatica.alis.studio.view.window.data.StooqPane;
import com.alphatica.alis.studio.view.window.trading.portfolio.PortfolioPane;
import com.alphatica.alis.studio.view.window.trading.strategies.StrategiesPane;
import com.formdev.flatlaf.FlatDarculaLaf;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import static com.alphatica.alis.studio.Constants.BACKEND_URL;
import static com.alphatica.alis.studio.Constants.STUDIO_NAME;
import static com.alphatica.alis.studio.view.tools.SwingHelper.createHtmlLinkLabel;
import static com.alphatica.alis.studio.view.tools.SwingHelper.createMenuActivatedPanel;

public class MainFrame extends JFrame {
	public final transient CardPane mainCardPane = new CardPane();

	public void init() {
		trySetupWindow();
		setJMenuBar(crateTopMenu());
		finish();
		setVisible(true);
	}

	private void finish() {
		mainCardPane.setMainJFrame(this);
	}

	private void trySetupWindow() {
		try {
			setupWindow();
		} catch (UnsupportedLookAndFeelException e) {
			ErrorDialog.showError("Unable to set look and feel", e.toString(), e);
		}
	}

	private void setupWindow() throws UnsupportedLookAndFeelException {
		setTitle(STUDIO_NAME);
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		UIManager.setLookAndFeel(new FlatDarculaLaf());
		Color fgColor = new Color(241, 234, 234);
		UIManager.put("Label.foreground", fgColor);
		UIManager.put("Button.foreground", fgColor);
		UIManager.put("TextField.foreground", fgColor);
		UIManager.put("TextArea.foreground", fgColor);
		UIManager.put("ComboBox.foreground", fgColor);
		UIManager.put("CheckBox.foreground", fgColor);
		UIManager.put("RadioButton.foreground", fgColor);
		UIManager.put("MenuItem.foreground", fgColor);
		UIManager.put("Menu.foreground", fgColor);
		UIManager.put("TabbedPane.foreground", fgColor);
		UIManager.put("Table.foreground", fgColor);
		UIManager.put("TableHeader.foreground", fgColor);
		UIManager.put("MenuBar.foreground", fgColor);
		UIManager.put("PopupMenu.foreground", fgColor);
		setLayout(new BorderLayout());
	}

	private JMenuBar crateTopMenu() {
		JMenuBar menuBar = new JMenuBar();

		addTradingMenu(menuBar);
		addAnalysisMenu(menuBar);
		addDataMenu(menuBar);
		addHelpMenu(menuBar);

		return menuBar;
	}

	private void addHelpMenu(JMenuBar menuBar) {
		JMenu helpMenu = new JMenu("Help");
		helpMenu.add(browserOpeningMenu("Website", "https://alphatica.com/alis/help"));
		helpMenu.add(licensePopup());
		helpMenu.add(browserOpeningMenu("Feature request", BACKEND_URL + "/feature-request/new"));
		helpMenu.add(createAboutMenu());
		menuBar.add(helpMenu);
	}

	private static JMenuItem createAboutMenu() {
		JPanel about = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.gridwidth = GridBagConstraints.REMAINDER; // Each component takes full width
		gbc.insets = new Insets(20, 0, 20, 0); // Vertical spacing
		gbc.anchor = GridBagConstraints.CENTER; // Center components

		JLabel studioLabel = new JLabel(STUDIO_NAME);

		JLabel webLabel = createHtmlLinkLabel("Web: alphatica.com", "https://alphatica.com/alis");

		JLabel licenseLabel = new JLabel("Licenced under GNU AFFERO GENERAL PUBLIC LICENSE 3.0");

		about.add(studioLabel, gbc);
		about.add(webLabel, gbc);
		about.add(licenseLabel, gbc);

		JFrame frame = new JFrame("Alis");
		JMenuItem menuItem = new JMenuItem("About");
		frame.add(about);
		frame.setSize(900, 300);
		frame.setLocationRelativeTo(null);
		menuItem.addActionListener(a -> frame.setVisible(true));
		return menuItem;
	}

	private JMenuItem licensePopup() {
		JMenuItem item = new JMenuItem("License");
		JFrame frame = new JFrame("License");

		JTextArea licenseTextArea = LicenseJTextField.createTextAreaWithLicense();

		JScrollPane scrollPane = new JScrollPane(licenseTextArea);
		frame.add(scrollPane);
		frame.setSize(1000, 800);
		frame.setLocationRelativeTo(null);

		item.addActionListener(a -> frame.setVisible(true));
		return item;
	}

	private JMenuItem browserOpeningMenu(String text, String url) {
		JMenuItem item = new JMenuItem(text);
		item.addActionListener(a -> {
			try {
				Desktop.getDesktop().browse(new URI(url));
			} catch (IOException | URISyntaxException ex) {
				ErrorDialog.showError("Unable to open browser", ex + " Go to " + url, ex);
			}
		});
		return item;
	}

	private void addTradingMenu(JMenuBar menuBar) {
		JMenu tradingMenu = new JMenu("Trading");
		tradingMenu.setMnemonic('T');
		tradingMenu.add(createMenuActivatedPanel("Portfolio", new PortfolioPane(), mainCardPane));
		tradingMenu.add(createMenuActivatedPanel("Strategies", new StrategiesPane(), mainCardPane));
		menuBar.add(tradingMenu);
	}

	private void addAnalysisMenu(JMenuBar menuBar) {
		JMenu analysisMenu = new JMenu("Analysis");
		analysisMenu.setMnemonic('A');
		analysisMenu.add(createMenuActivatedPanel("Find better exits", new FindBetterExitsPanel(), mainCardPane));
		menuBar.add(analysisMenu);
	}

	private void addDataMenu(JMenuBar menuBar) {
		JMenu dataMenu = new JMenu("Data");
		dataMenu.setMnemonic('D');
		dataMenu.add(createMenuActivatedPanel("Stooq", StooqPane.getPanel(), mainCardPane));
		menuBar.add(dataMenu);
	}

}
