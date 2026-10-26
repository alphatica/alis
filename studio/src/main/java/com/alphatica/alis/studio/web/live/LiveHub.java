package com.alphatica.alis.studio.web.live;

import com.alphatica.alis.studio.web.Json;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.javalin.config.RoutesConfig;
import io.javalin.websocket.WsContext;
import io.javalin.websocket.WsMessageContext;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * The single WebSocket each GUI tab keeps open. Every client receives the {@value #APP_TOPIC} topic;
 * other topics (e.g. job output) are delivered only after the client subscribes.
 * Requests that change state go through the REST API; this channel is for pushing data to the GUI fast.
 */
public final class LiveHub {
	public static final String PATH = "/ws";
	public static final String APP_TOPIC = "app";

	private final Map<String, SocketClient> clients = new ConcurrentHashMap<>();
	private final Map<String, Set<String>> subscriptions = new ConcurrentHashMap<>();
	private final Map<String, Function<String, Optional<TopicSource>>> resolvers = new ConcurrentHashMap<>();

	/**
	 * @param resolver finds the source for the topic name after the prefix, e.g. a job by its id
	 */
	public void addTopics(String prefix, Function<String, Optional<TopicSource>> resolver) {
		resolvers.put(prefix, resolver);
	}

	public void register(RoutesConfig routes) {
		routes.ws(PATH, ws -> {
			ws.onConnect(ctx -> {
				ctx.enableAutomaticPings();
				clients.put(ctx.sessionId(), new SocketClient(ctx));
			});
			ws.onMessage(this::onMessage);
			ws.onClose(this::disconnect);
			ws.onError(this::disconnect);
		});
	}

	public void broadcast(String event, Object data) {
		String json = Json.write(new LiveMessage(APP_TOPIC, event, data));
		clients.values().forEach(client -> client.send(json));
	}

	private void onMessage(WsMessageContext ctx) {
		SocketClient client = clients.get(ctx.sessionId());
		if (client == null) {
			return;
		}
		try {
			ClientCommand command = Json.read(ctx.message(), ClientCommand.class);
			if (ClientCommand.SUBSCRIBE.equals(command.op())) {
				subscribe(client, command.topic());
			} else if (ClientCommand.UNSUBSCRIBE.equals(command.op())) {
				unsubscribe(client, command.topic());
			}
		} catch (JsonProcessingException e) {
			System.err.println("Invalid WebSocket message: " + e.getMessage());
		}
	}

	private void subscribe(SocketClient client, String topic) {
		if (subscriptions.computeIfAbsent(client.id(), id -> ConcurrentHashMap.newKeySet()).add(topic)) {
			source(topic).ifPresent(source -> source.subscribe(client));
		}
	}

	private void unsubscribe(SocketClient client, String topic) {
		Set<String> topics = subscriptions.get(client.id());
		if (topics != null && topics.remove(topic)) {
			source(topic).ifPresent(source -> source.unsubscribe(client));
		}
	}

	private void disconnect(WsContext ctx) {
		SocketClient client = clients.remove(ctx.sessionId());
		Set<String> topics = subscriptions.remove(ctx.sessionId());
		if (client != null && topics != null) {
			topics.forEach(topic -> source(topic).ifPresent(source -> source.unsubscribe(client)));
		}
	}

	private Optional<TopicSource> source(String topic) {
		if (topic == null) {
			return Optional.empty();
		}
		return resolvers.entrySet().stream()
				.filter(entry -> topic.startsWith(entry.getKey()))
				.findFirst()
				.flatMap(entry -> entry.getValue().apply(topic.substring(entry.getKey().length())));
	}
}
