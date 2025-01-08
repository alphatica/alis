package com.alphatica.alis.studio.view.tools.components;

import javax.swing.JTextField;

public class DoubleTextField extends JTextField {
	private final String name;

	public DoubleTextField(String name, int columns) {
		super(columns);
		this.name = name;
	}

	public double getDoubleValue() {
		try {
			String text = getText().replace(",", ".").replace(" ", "").replace("_", "");
			double v = Double.parseDouble(text);
			if (v < 0) {
				throw new ComponentValidationException("Field `" + name + "` has negative value");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ComponentValidationException("Field `" + name + "` cannot be parsed to a number");
		}
	}
}
