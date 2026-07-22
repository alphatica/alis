package com.alphatica.alis.studio.tools;

import com.alphatica.alis.studio.view.tools.ErrorDialog;

import java.util.function.Supplier;

import static com.alphatica.alis.studio.view.tools.SwingHelper.runInBackground;

public class IfThenOrError {

	private IfThenOrError() {
	}

	public static <T> void ifThenOrError(Supplier<T> supplier, ThrowingConsumer<T> action, String errorTitle) {
		T value;
		try {
			value = supplier.get();
		} catch (Exception e) {
			ErrorDialog.showError(errorTitle, e.getMessage(), e);
			return;
		}
		if (value != null) {
			runInBackground(() -> {
				try {
					action.accept(value);
				} catch (Exception e) {
					ErrorDialog.showError(errorTitle, e.getMessage(), e);
				}
			});
		}
	}
}
