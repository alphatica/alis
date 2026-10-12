package com.alphatica.alis.studio.web.jobs;

import java.util.concurrent.atomic.AtomicBoolean;

public final class Job {
	private final String id;
	private final String type;
	private final JobChannel channel;
	private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
	private volatile Runnable cancelAction = () -> {
	};

	Job(String id, String type) {
		this.id = id;
		this.type = type;
		this.channel = new JobChannel(JobRegistry.TOPIC_PREFIX + id);
	}

	public String id() {
		return id;
	}

	public String type() {
		return type;
	}

	public JobChannel channel() {
		return channel;
	}

	public boolean isCancelRequested() {
		return cancelRequested.get();
	}

	/**
	 * Action stopping the underlying work, e.g. {@code optimizer::stop}.
	 */
	public void onCancel(Runnable action) {
		cancelAction = action;
		if (isCancelRequested()) {
			action.run();
		}
	}

	void cancel() {
		if (cancelRequested.compareAndSet(false, true)) {
			cancelAction.run();
		}
	}
}
