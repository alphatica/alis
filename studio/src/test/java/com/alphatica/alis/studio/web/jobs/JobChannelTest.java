package com.alphatica.alis.studio.web.jobs;

import com.alphatica.alis.studio.web.jobs.JobChannel.Snapshot;
import com.alphatica.alis.studio.web.live.LiveClient;
import com.alphatica.alis.studio.web.live.LiveMessage;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobChannelTest {
	private static final String TOPIC = "job/1";

	@Test
	void newSubscriberGetsEmptySnapshot() {
		JobChannel channel = new JobChannel(TOPIC);
		Recorder client = new Recorder();

		channel.subscribe(client);

		assertEquals(List.of(new LiveMessage(TOPIC, "snapshot", new Snapshot(Map.of(), Map.of()))), client.messages);
	}

	@Test
	void snapshotHoldsEverythingSentSoFar() {
		JobChannel channel = new JobChannel(TOPIC);
		channel.append("rows", "a");
		channel.replace("count", 1);
		channel.flush();
		channel.append("rows", "b");
		channel.replace("count", 2);
		channel.flush();
		Recorder client = new Recorder();

		channel.subscribe(client);

		assertEquals(List.of(new LiveMessage(TOPIC, "snapshot",
				new Snapshot(Map.of("rows", List.of("a", "b")), Map.of("count", 2)))), client.messages);
	}

	@Test
	void dataWaitingForFlushComesOnceAfterSnapshot() {
		JobChannel channel = new JobChannel(TOPIC);
		channel.append("rows", "a");
		channel.flush();
		channel.append("rows", "b");
		Recorder client = new Recorder();

		channel.subscribe(client);
		channel.flush();

		assertEquals(List.of(new LiveMessage(TOPIC, "snapshot", new Snapshot(Map.of("rows", List.of("a")), Map.of())),
				new LiveMessage(TOPIC, "rows", List.of("b"))), client.messages);
	}

	@Test
	void finishedJobSendsSnapshotThenDone() {
		JobChannel channel = new JobChannel(TOPIC);
		channel.replace("result", "x");
		channel.finish();
		Recorder client = new Recorder();

		channel.subscribe(client);
		channel.flush();

		assertEquals(List.of(new LiveMessage(TOPIC, "snapshot", new Snapshot(Map.of(), Map.of("result", "x"))),
				new LiveMessage(TOPIC, "done", null)), client.messages);
	}

	@Test
	void unsubscribedClientGetsNothingMore() {
		JobChannel channel = new JobChannel(TOPIC);
		Recorder client = new Recorder();
		channel.subscribe(client);

		channel.unsubscribe(client);
		channel.append("rows", "a");
		channel.flush();

		assertEquals(1, client.messages.size());
	}

	private static final class Recorder implements LiveClient {
		final List<LiveMessage> messages = new ArrayList<>();

		@Override
		public void send(LiveMessage message) {
			messages.add(message);
		}

		@Override
		public boolean isOpen() {
			return true;
		}
	}
}
