package com.alphatica.alis.studio.view.window.trading.portfolio;

import com.alphatica.alis.studio.dao.DaoException;
import com.alphatica.alis.studio.logic.trading.account.AccountProvider;
import com.alphatica.alis.studio.view.tools.components.DoubleTextField;
import com.alphatica.alis.studio.view.tools.components.MarketTextField;
import com.alphatica.alis.studio.view.tools.components.SmartComboBox;
import com.alphatica.alis.studio.view.tools.components.TimeTextField;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.Trade;
import com.alphatica.alis.trading.order.Direction;

import javax.swing.*;
import java.awt.*;

import static com.alphatica.alis.studio.tools.IfThenOrError.ifThenOrError;

public class AddTradeFrame extends JFrame {

	private final TimeTextField timeTextField;
	private final DoubleTextField priceTextField;
	private final DoubleTextField quantityTextField;
	private final DoubleTextField commissionTextField;
	private final MarketTextField marketTextField;
	private final SmartComboBox<Direction> buySellComboBox = new SmartComboBox<>();

	public AddTradeFrame() {
		// Initialize text fields, relying on FlatLaf for scaling
		timeTextField = new TimeTextField("time", 12); // Increased columns for better width
		priceTextField = new DoubleTextField("price", 12);
		quantityTextField = new DoubleTextField("quantity", 12);
		commissionTextField = new DoubleTextField("commission", 12);
		marketTextField = new MarketTextField(12);

		init();
		setupComponents();
		setVisible(true);
	}

	private void init() {
		setTitle("Add Trade");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout());
		setLocationRelativeTo(null);
		setMinimumSize(new Dimension(350, 300));
	}

	private void setupComponents() {
		JPanel mainPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 10, 10, 10);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;

		// Add fields
		addField(mainPanel, gbc, 0, "Time:", timeTextField);
		addField(mainPanel, gbc, 1, "Market:", marketTextField);
		addField(mainPanel, gbc, 2, "Order:", buySellComboBox);
		addField(mainPanel, gbc, 3, "Price:", priceTextField);
		addField(mainPanel, gbc, 4, "Quantity:", quantityTextField);
		addField(mainPanel, gbc, 5, "Commission:", commissionTextField);

		// Populate buy/sell combo box
		buySellComboBox.addOption("Buy", () -> Direction.BUY);
		buySellComboBox.addOption("Sell", () -> Direction.SELL);

		// Create button panel
		JPanel buttonPanel = createButtonPanel();

		add(mainPanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);

		pack();
	}

	private void addField(JPanel panel, GridBagConstraints gbc, int y, String label, JComponent component) {
		gbc.gridx = 0;
		gbc.gridy = y;
		gbc.weightx = 0.3;
		JLabel jLabel = new JLabel(label);
		panel.add(jLabel, gbc);

		gbc.gridx = 1;
		gbc.weightx = 0.7;
		panel.add(component, gbc);
	}

	private JPanel createButtonPanel() {
		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
		JButton submitButton = new JButton("Add");
		submitButton.addActionListener(e -> addTrade());
		buttonPanel.add(submitButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> dispose());
		buttonPanel.add(cancelButton);

		return buttonPanel;
	}

	private void addTrade() {
		ifThenOrError(() -> new AccountAction(
				timeTextField.getTime(),
				new Trade(
						marketTextField.getMarketName(),
						buySellComboBox.getValue(),
						priceTextField.getDoubleValue(),
						(int) Math.round(quantityTextField.getDoubleValue()),
						commissionTextField.getDoubleValue()
				)
		), this::saveAction, "Unable to add trade");
	}

	private void saveAction(AccountAction action) throws AccountActionException, DaoException {
		AccountProvider.saveAction(action);
		dispose();
	}
}