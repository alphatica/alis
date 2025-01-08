package com.alphatica.alis.studio.view.window.trading.portfolio;

import com.alphatica.alis.data.time.Time;
import com.alphatica.alis.studio.dao.DaoException;
import com.alphatica.alis.studio.logic.trading.account.AccountProvider;
import com.alphatica.alis.studio.view.tools.components.DoubleTextField;
import com.alphatica.alis.studio.view.tools.components.SmartComboBox;
import com.alphatica.alis.studio.view.tools.components.TimeTextField;
import com.alphatica.alis.trading.account.actions.AccountAction;
import com.alphatica.alis.trading.account.actions.AccountActionException;
import com.alphatica.alis.trading.account.actions.AccountActionType;
import com.alphatica.alis.trading.account.actions.Deposit;
import com.alphatica.alis.trading.account.actions.Withdrawal;

import javax.swing.*;
import java.awt.*;

import static com.alphatica.alis.studio.tools.IfThenOrError.ifThenOrError;

public class AddRemoveCashFrame extends JFrame {

	private final TimeTextField timeTextField;
	private final DoubleTextField amountTextField;
	private final SmartComboBox<AccountActionType> addRemoveCashComboBox = new SmartComboBox<>();

	public AddRemoveCashFrame() {
		// Initialize text fields, relying on FlatLaf for scaling
		timeTextField = new TimeTextField("time", 10);
		amountTextField = new DoubleTextField("amount", 10);

		init();
		setupComponents();
		setVisible(true);
	}

	private void init() {
		setTitle("Add / Remove Cash");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout());
		setLocationRelativeTo(null);
		setMinimumSize(new Dimension(350, 250));
	}

	private void setupComponents() {
		JPanel mainPanel = new JPanel(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(10, 10, 10, 10);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.anchor = GridBagConstraints.WEST;

		// Add fields
		addField(mainPanel, gbc, 0, "Time:", timeTextField);
		addField(mainPanel, gbc, 1, "Type:", addRemoveCashComboBox);
		addField(mainPanel, gbc, 2, "Amount:", amountTextField);

		// Populate add/remove combo box
		addRemoveCashComboBox.addOption("Deposit", () -> new Deposit(amountTextField.getDoubleValue()));
		addRemoveCashComboBox.addOption("Withdrawal", () -> new Withdrawal(amountTextField.getDoubleValue()));

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
		JButton submitButton = new JButton("Submit");
		submitButton.addActionListener(e -> addAction());
		buttonPanel.add(submitButton);

		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> dispose());
		buttonPanel.add(cancelButton);

		return buttonPanel;
	}

	private void addAction() {
		ifThenOrError(this::createAction, this::saveAction, "Unable to deposit / withdraw cash");
	}

	private AccountAction createAction() {
		AccountActionType actionType = addRemoveCashComboBox.getValue();
		Time time = timeTextField.getTime();
		return new AccountAction(time, actionType);
	}

	private void saveAction(AccountAction action) throws AccountActionException, DaoException {
		AccountProvider.saveAction(action);
		dispose();
	}
}