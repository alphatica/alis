package com.alphatica.alis.studio.state;

import com.alphatica.alis.studio.view.tools.SwingHelper;

import javax.swing.JLabel;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static com.alphatica.alis.studio.view.tools.SwingHelper.buildUiThread;

public class ChangeListeners {
	private static final Map<StateChange, List<Runnable>> listeners = new EnumMap<>(StateChange.class);

	private ChangeListeners() {
	}

	public static void addListener(StateChange stateChange, Runnable runnable) {
		listeners.computeIfAbsent(stateChange, k -> new ArrayList<>()).add(runnable);
	}

	public static void publish(StateChange stateChange) {
		List<Runnable> eventListeners = listeners.get(stateChange);
		if (eventListeners != null) {
			eventListeners.forEach(SwingHelper::runUiThread);
		}
	}

	public static void bindLabelToEvent(JLabel label, StateChange stateChange, Supplier<String> supplier) {
		addListener(stateChange, buildUiThread(() -> label.setText(supplier.get())));
	}
}
