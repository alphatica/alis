package com.alphatica.alis.studio.web.jobs;

import java.util.concurrent.atomic.AtomicBoolean;

public final class Job {
	private final String id;
	private final String type;
	private final Object input;
	private final JobChannel channel;
	private final AtomicBoolean cancelRequested = new AtomicBoolean(false);
	private volatile Runnable cancelAction = () -> {
	};

	Job(String id, String type, Object input) {
		this.id = id;
		this.type = type;
		this.input = input;
		this.channel = new JobChannel(JobRegistry.TOPIC_PREFIX + id);
	}

	public String id() {
		return id;
	}

	public String type() {
		return type;
	}

	/**
	 * The request the job was started with, shown by the GUI next to the job's output; null when there is none.
	 */
	public Object input() {
		return input;
	}

	public JobChannel channel() {
		return channel;
	}

	public boolean isCancelRequested() {
		return cancelRequested.get();
	}

	/**
	 * Action stopping the underlying work, e.g. {@code optimizer::stop}; run again on every further request, so it
	 * must allow that.
	 */
	public void onCancel(Runnable action) {
		cancelAction = action;
		if (isCancelRequested()) {
			action.run();
		}
	}

	/**
	 * Runs the cancel action on every request while the job runs, so a job of several stages can stop each one;
	 * nothing after the job ends.
	 */
	void cancel() {
		if (channel.isFinished()) {
			return;
		}
		cancelRequested.set(true);
		cancelAction.run();
	}
}
