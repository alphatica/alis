package com.alphatica.alis.studio.view.tools.components;

import com.alphatica.alis.data.market.MarketName;

import javax.swing.JTextField;

public class MarketTextField extends JTextField {

	public MarketTextField(int columns) {
		super(columns);
	}

	public MarketName getMarketName() {
		return new MarketName(getText());
	}
}
