package com.alphatica.alis.studio.view.tools;

import com.alphatica.alis.studio.state.StateChange;

import javax.swing.JLabel;
import java.util.function.Supplier;

import static com.alphatica.alis.studio.state.ChangeListeners.addListener;

public final class SwingChangeListeners {
	private SwingChangeListeners() {
	}

	public static void addUiListener(StateChange stateChange, Runnable listener) {
		addListener(stateChange, () -> SwingHelper.runUiThread(listener));
	}

	public static void bindLabelToEvent(JLabel label, StateChange stateChange, Supplier<String> supplier) {
		addUiListener(stateChange, () -> label.setText(supplier.get()));
	}
}
