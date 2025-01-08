package com.alphatica.alis.studio.view.tools.models;

import javax.swing.table.DefaultTableModel;

public class ReadOnlyTableModel extends DefaultTableModel {

	public ReadOnlyTableModel() {
	}

	public ReadOnlyTableModel(Object[][] objects, String[] columns) {
		super(objects, columns);
	}

	@Override
	public boolean isCellEditable(int row, int column) {
		return false;
	}
}
