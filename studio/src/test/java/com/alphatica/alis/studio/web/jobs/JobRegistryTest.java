package com.alphatica.alis.studio.web.jobs;

import com.alphatica.alis.studio.web.ErrorReports;
import com.alphatica.alis.studio.web.ValidationException;
import com.alphatica.alis.studio.web.jobs.JobRegistry.JobInfo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobRegistryTest {
	private final JobRegistry registry = new JobRegistry(new ErrorReports());

	@Test
	void listsRunningJobWithItsInput() throws InterruptedException {
		CountDownLatch release = new CountDownLatch(1);
		Job job = registry.start("optimization", "request", running -> release.await());
		try {
			assertEquals(List.of(new JobInfo(job.id(), "optimization", "request", true, false)), registry.list());
		} finally {
			release.countDown();
		}
		awaitFinished(job);
	}

	@Test
	void keepsFinishedJob() throws InterruptedException {
		Job job = registry.start("signals", "request", running -> running.channel().replace("result", 1));
		awaitFinished(job);

		assertEquals(List.of(new JobInfo(job.id(), "signals", "request", false, false)), registry.list());
	}

	@Test
	void showsCancelRequest() throws InterruptedException {
		CountDownLatch release = new CountDownLatch(1);
		Job job = registry.start("optimization", null, running -> release.await());
		job.cancel();
		try {
			assertTrue(registry.list().getFirst().cancelRequested());
		} finally {
			release.countDown();
		}
		awaitFinished(job);
	}

	@Test
	void newJobReplacesFinishedOneOfSameType() throws InterruptedException {
		Job first = registry.start("backtest", "first", running -> {
		});
		awaitFinished(first);
		Job second = registry.start("backtest", "second", running -> {
		});
		awaitFinished(second);
		Job other = registry.start("signals", null, running -> {
		});
		awaitFinished(other);

		List<String> ids = registry.list().stream().map(JobInfo::id).sorted().toList();
		assertEquals(List.of(second.id(), other.id()).stream().sorted().toList(), ids);
	}

	@Test
	void secondRunningJobOfSameTypeIsRejected() throws InterruptedException {
		CountDownLatch release = new CountDownLatch(1);
		Job job = registry.start("optimization", null, running -> release.await());
		try {
			assertThrows(ValidationException.class, () -> registry.start("optimization", null, running -> {
			}));
		} finally {
			release.countDown();
		}
		awaitFinished(job);
	}

	private static void awaitFinished(Job job) throws InterruptedException {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (!job.channel().isFinished()) {
			if (System.nanoTime() > deadline) {
				throw new AssertionError("Job did not finish");
			}
			Thread.sleep(5);
		}
	}
}
