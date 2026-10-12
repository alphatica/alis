package com.alphatica.alis.studio.web;

/**
 * Stops the application shortly after the current response has been sent.
 */
final class Shutdown {
	private static final long DELAY_MILLIS = 500;

	private Shutdown() {
	}

	static void afterResponse() {
		Thread.ofPlatform().start(() -> {
			try {
				Thread.sleep(DELAY_MILLIS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			System.exit(0);
		});
	}
}
