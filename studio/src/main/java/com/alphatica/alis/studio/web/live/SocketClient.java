package com.alphatica.alis.studio.web.live;

import com.alphatica.alis.studio.web.Json;
import io.javalin.websocket.WsContext;

/**
 * A GUI tab connected to the WebSocket. Messages are sent one at a time.
 */
final class SocketClient implements LiveClient {
	private final WsContext ctx;

	SocketClient(WsContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public void send(LiveMessage message) {
		send(Json.write(message));
	}

	synchronized void send(String json) {
		if (isOpen()) {
			ctx.send(json);
		}
	}

	@Override
	public boolean isOpen() {
		return ctx.session.isOpen();
	}

	String id() {
		return ctx.sessionId();
	}
}
