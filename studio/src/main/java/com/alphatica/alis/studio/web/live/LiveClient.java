package com.alphatica.alis.studio.web.live;

import com.alphatica.alis.studio.web.Json;
import io.javalin.websocket.WsContext;

/**
 * One open GUI tab. Messages may come from many threads; they are sent one at a time.
 */
public final class LiveClient {
	private final WsContext ctx;

	LiveClient(WsContext ctx) {
		this.ctx = ctx;
	}

	public void send(LiveMessage message) {
		send(Json.write(message));
	}

	synchronized void send(String json) {
		if (isOpen()) {
			ctx.send(json);
		}
	}

	public boolean isOpen() {
		return ctx.session.isOpen();
	}

	String id() {
		return ctx.sessionId();
	}
}
