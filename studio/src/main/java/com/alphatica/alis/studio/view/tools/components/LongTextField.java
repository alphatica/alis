package com.alphatica.alis.studio.view.tools.components;

import javax.swing.JTextField;

import static java.lang.String.format;

public class LongTextField extends JTextField {
	private final String name;

	public LongTextField(String name, int columns) {
		super(columns);
		this.name = name;
	}

	public long getValue() {
		try {
			String text = getText().replace(",", ".").replace(" ", "").replace("_", "");
			long v = Long.parseLong(text);
			if (v < 0) {
				throw new ComponentValidationException("Field `" + name + "` has negative value");
			}
			return v;
		} catch (NumberFormatException e) {
			throw new ComponentValidationException("Field `" + name + "` cannot be parsed to a number");
		}
	}

	public void setValue(long count) {
		setText(format("%d", count));
	}
}
