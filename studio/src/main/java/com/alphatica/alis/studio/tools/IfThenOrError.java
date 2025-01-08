package com.alphatica.alis.studio.tools;

import com.alphatica.alis.studio.view.tools.ErrorDialog;

import java.util.function.Supplier;

import static com.alphatica.alis.studio.tools.GlobalThreadExecutor.GLOBAL_EXECUTOR;

public class IfThenOrError {

	private IfThenOrError() {
	}

	public static <T> void ifThenOrError(Supplier<T> supplier, ThrowingConsumer<T> action, String errorTitle) {
		GLOBAL_EXECUTOR.execute(() -> {
			try {
				T v = supplier.get();
				if (v != null) {
					action.accept(v);
				}
			} catch (Exception e) {
				ErrorDialog.showError(errorTitle, e.getMessage(), e);
			}
		});
	}
}
