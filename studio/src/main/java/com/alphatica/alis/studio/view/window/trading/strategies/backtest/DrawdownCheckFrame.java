package com.alphatica.alis.studio.view.window.trading.strategies.backtest;

import com.alphatica.alis.studio.view.tools.SwingHelper;
import com.alphatica.alis.trading.account.RecentDrawdownChecker;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.function.Consumer;

import static com.alphatica.alis.studio.tools.GlobalThreadExecutor.GLOBAL_EXECUTOR;
import static java.lang.String.format;

public class DrawdownCheckFrame extends JFrame {

	private final JTextArea textArea = new JTextArea();

	DrawdownCheckFrame(List<Double> navHistory) {
		init();
		startWork(navHistory);
	}

	private void init() {
		setTitle("Drawdown Check");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

		// Configure JTextArea
		textArea.setEditable(false);
		textArea.setCaretColor(null);
		textArea.setRows(20); // Default height for reasonable frame size
		textArea.setColumns(60); // Wider to accommodate longer text rows
		textArea.setLineWrap(false); // Preserve text formatting

		// Create panel with padding
		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Add JScrollPane
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setBorder(BorderFactory.createEmptyBorder()); // Clean look
		panel.add(scrollPane, BorderLayout.CENTER);

		add(panel, BorderLayout.CENTER);

		// Set size constraints
		setMinimumSize(new Dimension(400, 300));
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setMaximumSize(new Dimension((int) (screenSize.width * 0.8), (int) (screenSize.height * 0.8)));

		// Size the frame first, then center it
		pack();
		setLocationRelativeTo(null); // Center the frame's center on the screen

		setVisible(true);
	}

	private void startWork(List<Double> navHistory) {
		GLOBAL_EXECUTOR.execute(() -> doWork(navHistory));
	}

	private void doWork(List<Double> navHistory) {
		Consumer<String> stringConsumer = (s) -> SwingHelper.runUiThread(() -> textArea.append(format("%s%n", s)));
		RecentDrawdownChecker.checkDrawdown(navHistory, stringConsumer);
	}
}