package com.alphatica.alis.studio.view.tools.components;

import javax.swing.JComboBox;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SmartComboBox<T> extends JComboBox<String> {
	private final List<Supplier<T>> suppliers = new ArrayList<>();

	public void addOption(String display, Supplier<T> supplier) {
		this.addItem(display);
		suppliers.add(supplier);
	}

	public T getValue() {
		int selected = this.getSelectedIndex();
		return suppliers.get(selected).get();
	}
}
