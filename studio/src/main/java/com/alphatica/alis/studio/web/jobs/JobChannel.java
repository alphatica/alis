package com.alphatica.alis.studio.web.jobs;

import com.alphatica.alis.studio.web.live.LiveClient;
import com.alphatica.alis.studio.web.live.LiveMessage;
import com.alphatica.alis.studio.web.live.TopicSource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Buffers data produced by a job and sends it to subscribed GUI clients in batches.
 * Late subscribers first receive everything sent so far.
 */
public final class JobChannel implements TopicSource {
	static final String DONE_EVENT = "done";

	private final String topic;
	private final Map<String, List<Object>> pendingAppends = new LinkedHashMap<>();
	private final Map<String, Object> pendingReplaces = new LinkedHashMap<>();
	private final Map<String, List<Object>> appendHistory = new LinkedHashMap<>();
	private final Map<String, Object> replaceHistory = new LinkedHashMap<>();
	private final Set<LiveClient> clients = new CopyOnWriteArraySet<>();
	private boolean finished;

	JobChannel(String topic) {
		this.topic = topic;
	}

	/**
	 * Item added to existing content, e.g. a table row. Clients receive a list of items per event.
	 */
	public synchronized void append(String event, Object item) {
		pendingAppends.computeIfAbsent(event, key -> new ArrayList<>()).add(item);
	}

	/**
	 * Value replacing the previous one, e.g. a progress counter. Only the latest value is sent.
	 */
	public synchronized void replace(String event, Object value) {
		pendingReplaces.put(event, value);
	}

	@Override
	public synchronized void subscribe(LiveClient client) {
		appendHistory.forEach((event, items) -> client.send(message(event, items)));
		replaceHistory.forEach((event, value) -> client.send(message(event, value)));
		if (finished) {
			client.send(message(DONE_EVENT, null));
		} else {
			clients.add(client);
		}
	}

	@Override
	public void unsubscribe(LiveClient client) {
		clients.remove(client);
	}

	synchronized void flush() {
		pendingAppends.forEach((event, items) -> {
			send(message(event, List.copyOf(items)));
			appendHistory.computeIfAbsent(event, key -> new ArrayList<>()).addAll(items);
		});
		pendingReplaces.forEach((event, value) -> {
			send(message(event, value));
			replaceHistory.put(event, value);
		});
		pendingAppends.clear();
		pendingReplaces.clear();
	}

	synchronized void finish() {
		flush();
		finished = true;
		send(message(DONE_EVENT, null));
		clients.clear();
	}

	synchronized boolean isFinished() {
		return finished;
	}

	private LiveMessage message(String event, Object data) {
		return new LiveMessage(topic, event, data);
	}

	private void send(LiveMessage message) {
		clients.removeIf(client -> !client.isOpen());
		clients.forEach(client -> client.send(message));
	}
}
