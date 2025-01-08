package com.alphatica.alis.studio.view.window.trading.strategies.backtest;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.state.AppState;
import com.alphatica.alis.studio.view.tools.SwingHelper;
import com.alphatica.alis.studio.view.tools.components.LongTextField;
import com.alphatica.alis.trading.account.Account;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static com.alphatica.alis.studio.tools.GlobalThreadExecutor.GLOBAL_EXECUTOR;
import static java.lang.String.format;

public class CompareWithRandomFrame extends JFrame {

	private final JTextArea textArea = new JTextArea();
	private final LongTextField numberOfIterations = new LongTextField("Iterations", 6);
	private final JButton startButton = new JButton("Start");
	private final JButton stopButton = new JButton("Stop");
	private RandomStrategyComparator randomStrategyComparator;

	public CompareWithRandomFrame(Account account, Time startTime, Time endTime, double commissionRate, double initialCapital) {
		init();
		startButton.addActionListener(a -> start(account, startTime, endTime, commissionRate, initialCapital));
		stopButton.addActionListener(a -> tryStop());
	}

	private void tryStop() {
		if (randomStrategyComparator != null) {
			randomStrategyComparator.stop();
		}
	}

	private void start(Account account, Time startTime, Time endTime, double commissionRate, double initialCapital) {
		randomStrategyComparator = new RandomStrategyComparator(this::showMessage, (int) numberOfIterations.getValue(), initialCapital, commissionRate, startTime, endTime, AppState.getMarketData());
		enableButtons(false);
		GLOBAL_EXECUTOR.execute(() -> {
			randomStrategyComparator.compare(account);
			SwingHelper.runUiThread(() -> enableButtons(true));
		});
	}

	private void init() {
		setTitle("Compare to random");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				tryStop();
			}
		});

		// Setup main panel
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		// Setup control panel at the top
		JPanel controlPanel = setupControlPanel();
		mainPanel.add(controlPanel, BorderLayout.NORTH);

		// Setup text area and scroll pane
		setupTextArea();
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setBorder(BorderFactory.createEmptyBorder()); // Clean look
		mainPanel.add(scrollPane, BorderLayout.CENTER);

		add(mainPanel, BorderLayout.CENTER);

		// Set size constraints
		setMinimumSize(new Dimension(400, 300));
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setMaximumSize(new Dimension((int) (screenSize.width * 0.8), (int) (screenSize.height * 0.8)));

		// Size and center the frame
		pack();
		setLocationRelativeTo(null); // Center the frame's center on the screen

		enableButtons(true);
		setVisible(true);
	}

	private JPanel setupControlPanel() {
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
		controlPanel.add(new JLabel("Iterations:"));
		numberOfIterations.setText("1 000");
		controlPanel.add(numberOfIterations);
		controlPanel.add(startButton);
		controlPanel.add(stopButton);
		return controlPanel;
	}

	private void setupTextArea() {
		textArea.setEditable(false);
		textArea.setCaretColor(null);
		textArea.setRows(20); // Default height for reasonable frame size
		textArea.setColumns(60); // Wider to accommodate longer text rows
		textArea.setLineWrap(false); // Preserve text formatting
	}

	private void enableButtons(boolean ableToStart) {
		startButton.setEnabled(ableToStart);
		stopButton.setEnabled(!ableToStart);
	}

	private void showMessage(String message) {
		SwingHelper.runUiThread(() -> textArea.append(format("%s%n", message)));
	}
}