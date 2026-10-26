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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Long-running jobs, at most one running job per type. The GUI subscribes to topic {@code job/<id>} on the
 * WebSocket; output is pushed every 200 ms. The last job of every type is kept in memory with its output, also after
 * it ends, so a GUI opened later finds it ({@code GET /api/jobs}) and shows it again.
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
		routes.get("/api/jobs", ctx -> ctx.json(list()));
		routes.post("/api/jobs/{id}/cancel", this::cancel);
		hub.addTopics(TOPIC_PREFIX, this::topic);
	}

	public Job start(String type, JobWork work) {
		return start(type, null, work);
	}

	/**
	 * @param input the request the job is started with, given back to the GUI by {@code GET /api/jobs}
	 */
	public synchronized Job start(String type, Object input, JobWork work) {
		Job previous = latestByType.get(type);
		if (previous != null && !previous.channel().isFinished()) {
			throw new ValidationException("Already running: " + type);
		}
		if (previous != null) {
			jobsById.remove(previous.id());
		}
		Job job = new Job(UUID.randomUUID().toString(), type, input);
		jobsById.put(job.id(), job);
		latestByType.put(type, job);
		Thread.ofVirtual().name("job-" + type).start(() -> execute(job, work));
		return job;
	}

	/**
	 * The last job of every type.
	 */
	List<JobInfo> list() {
		return latestByType.values().stream().map(JobInfo::of).toList();
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

	/**
	 * @param input the request the job was started with, null when there is none
	 */
	record JobInfo(String id, String type, Object input, boolean running, boolean cancelRequested) {

		static JobInfo of(Job job) {
			return new JobInfo(job.id(), job.type(), job.input(), !job.channel().isFinished(), job.isCancelRequested());
		}
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
