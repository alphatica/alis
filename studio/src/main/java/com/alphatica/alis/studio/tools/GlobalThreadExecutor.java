package com.alphatica.alis.studio.tools;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class GlobalThreadExecutor {
	public static final Executor GLOBAL_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

	private GlobalThreadExecutor() {
	}
}
