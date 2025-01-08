package com.alphatica.alis.studio.view.tools.components;

import com.alphatica.alis.data.time.Time;

import javax.swing.JTextField;

public class TimeTextField extends JTextField {
	private final String name;

	public TimeTextField(String name, int columns) {
		super(columns);
		this.name = name;
	}

	public Time getTime() {
		try {
			String text = getText().replace("-", "").replace(" ", "").replace("_", "");
			if (!text.isEmpty()) {
				long v = Long.parseLong(text);
				return new Time(v);
			} else {
				return new Time(0);
			}

		} catch (NumberFormatException e) {
			throw new ComponentValidationException("Field `" + name + "` cannot be parsed to a number");
		}
	}
}
