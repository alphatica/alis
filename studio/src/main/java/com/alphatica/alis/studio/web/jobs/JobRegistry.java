package com.alphatica.alis.studio.web.jobs;

import com.alphatica.alis.studio.web.ErrorMessage;
import com.alphatica.alis.studio.web.ErrorReports;
import com.alphatica.alis.studio.web.ValidationException;
import com.alphatica.alis.studio.web.live.LiveClient;
import com.alphatica.alis.studio.web.live.LiveHub;
import com.alphatica.alis.studio.web.live.LiveMessage;
import com.alphatica.alis.studio.web.live.TopicSource;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Long-running jobs, at most one running job per type. The GUI subscribes to topic {@code job/<id>} on the
 * WebSocket; output is pushed every 200 ms.
 */
public final class JobRegistry {
	public static final String ERROR_EVENT = "error";
	static final String TOPIC_PREFIX = "job/";
	private static final long FLUSH_INTERVAL_MILLIS = 200;

	private final ErrorReports errorReports;
	private final Map<String, Job> jobsById = new ConcurrentHashMap<>();
	private final Map<String, Job> latestByType = new ConcurrentHashMap<>();
	private final ScheduledExecutorService flusher = Executors.newSingleThreadScheduledExecutor(
			runnable -> Thread.ofPlatform().daemon().name("job-flusher").unstarted(runnable));

	public JobRegistry(ErrorReports errorReports) {
		this.errorReports = errorReports;
		flusher.scheduleAtFixedRate(this::flushAll, FLUSH_INTERVAL_MILLIS, FLUSH_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
	}

	public void register(RoutesConfig routes, LiveHub hub) {
		routes.post("/api/jobs/{id}/cancel", this::cancel);
		hub.addTopics(TOPIC_PREFIX, this::topic);
	}

	public synchronized Job start(String type, JobWork work) {
		Job previous = latestByType.get(type);
		if (previous != null && !previous.channel().isFinished()) {
			throw new ValidationException("Already running: " + type);
		}
		if (previous != null) {
			jobsById.remove(previous.id());
		}
		Job job = new Job(UUID.randomUUID().toString(), type);
		jobsById.put(job.id(), job);
		latestByType.put(type, job);
		Thread.ofVirtual().name("job-" + type).start(() -> execute(job, work));
		return job;
	}

	private void execute(Job job, JobWork work) {
		try {
			work.run(job);
		} catch (ValidationException e) {
			job.channel().append(ERROR_EVENT, ErrorMessage.invalidInput(e.getMessage()));
		} catch (Exception e) {
			job.channel().append(ERROR_EVENT, ErrorMessage.unexpected(errorReports, e));
		} finally {
			job.channel().finish();
		}
	}

	/**
	 * An unknown job (e.g. replaced by a newer one of the same type) is reported as finished.
	 */
	private Optional<TopicSource> topic(String jobId) {
		Job job = jobsById.get(jobId);
		return Optional.of(job == null ? new FinishedJob(TOPIC_PREFIX + jobId) : job.channel());
	}

	private void cancel(Context ctx) {
		Job job = jobsById.get(ctx.pathParam("id"));
		if (job != null) {
			job.cancel();
		}
		ctx.status(HttpStatus.NO_CONTENT);
	}

	private void flushAll() {
		jobsById.values().forEach(job -> job.channel().flush());
	}

	private record FinishedJob(String topic) implements TopicSource {
		@Override
		public void subscribe(LiveClient client) {
			client.send(new LiveMessage(topic, JobChannel.DONE_EVENT, null));
		}

		@Override
		public void unsubscribe(LiveClient client) {
			// Nothing to stop
		}
	}
}
