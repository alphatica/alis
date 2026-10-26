package com.alphatica.alis.studio.web;

import java.awt.SplashScreen;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The startup image shown by the java launcher (SplashScreen-Image in the manifest, or -splash for jpackage).
 * It stays up until the browser makes its first request, or until the fallback timeout.
 */
final class Splash {
	private static final long FALLBACK_CLOSE_MS = 30_000;
	private static final AtomicBoolean CLOSED = new AtomicBoolean();

	private Splash() {
	}

	/** Closes the splash later even if the browser never connects. */
	static void closeLater() {
		Thread thread = new Thread(() -> {
			try {
				Thread.sleep(FALLBACK_CLOSE_MS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			close();
		}, "splash-timeout");
		thread.setDaemon(true);
		thread.start();
	}

	static void close() {
		if (!CLOSED.compareAndSet(false, true)) {
			return;
		}
		try {
			SplashScreen splash = SplashScreen.getSplashScreen();
			if (splash != null) {
				splash.close();
			}
		} catch (UnsupportedOperationException | IllegalStateException e) {
			// headless or already closed
		}
	}
}
