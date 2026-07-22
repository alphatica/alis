package com.alphatica.alis.studio.view.tools;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SwingHelperTest {
	static {
		System.setProperty("java.awt.headless", "true");
	}

	@Test
	void shouldRunUiTaskImmediatelyWhenAlreadyOnEdt() throws Exception {
		SwingUtilities.invokeAndWait(() -> {
			AtomicBoolean completed = new AtomicBoolean(false);

			SwingHelper.runUiThread(() -> {
				assertTrue(SwingUtilities.isEventDispatchThread());
				completed.set(true);
			});

			assertTrue(completed.get());
		});
	}

	@Test
	void shouldRunTaskInBackgroundAndCompletionOnEdt() throws Exception {
		CountDownLatch completed = new CountDownLatch(1);
		AtomicBoolean taskOnEdt = new AtomicBoolean(true);
		AtomicBoolean completionOnEdt = new AtomicBoolean(false);

		SwingHelper.runInBackground(
				() -> taskOnEdt.set(SwingUtilities.isEventDispatchThread()),
				() -> {
					completionOnEdt.set(SwingUtilities.isEventDispatchThread());
					completed.countDown();
				});

		assertTrue(completed.await(5, TimeUnit.SECONDS));
		assertFalse(taskOnEdt.get());
		assertTrue(completionOnEdt.get());
	}
}
