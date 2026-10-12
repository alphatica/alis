package com.alphatica.alis.studio.web;

import com.alphatica.alis.studio.web.live.LiveHub;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HandlerType;

import java.util.Set;

/**
 * Every request passes through here; this is the place for authentication when Studio is opened to other machines.
 * For now:
 * - the Host header must be one of ours, so a page from another site cannot reach the API through DNS rebinding,
 * - requests that change state and WebSocket connections must come from our own pages (Origin), so another site
 *   open in the same browser cannot send them.
 */
final class RequestGuard {
	private static final Set<HandlerType> SAFE_METHODS = Set.of(HandlerType.GET, HandlerType.HEAD, HandlerType.OPTIONS);

	private final Set<String> allowedHosts;

	/**
	 * @param allowedHosts values of the Host header, e.g. {@code 127.0.0.1:7070}
	 */
	RequestGuard(Set<String> allowedHosts) {
		this.allowedHosts = allowedHosts;
	}

	void register(RoutesConfig routes) {
		routes.before(this::checkHttp);
		routes.wsBeforeUpgrade(LiveHub.PATH, this::checkWebSocket);
	}

	private void checkHttp(Context ctx) {
		checkHost(ctx);
		if (!SAFE_METHODS.contains(ctx.method())) {
			checkOrigin(ctx);
		}
	}

	private void checkWebSocket(Context ctx) {
		checkHost(ctx);
		checkOrigin(ctx);
	}

	private void checkHost(Context ctx) {
		if (!allowedHosts.contains(ctx.host())) {
			throw new ForbiddenResponse("Unknown host");
		}
	}

	/**
	 * Browsers always send Origin with cross-site requests that change state and with WebSocket connections.
	 * Requests without it come from other tools (e.g. curl), not from a page in the browser.
	 */
	private void checkOrigin(Context ctx) {
		String origin = ctx.header("Origin");
		if (origin != null && !allowedHosts.contains(origin.replaceFirst("^https?://", ""))) {
			throw new ForbiddenResponse("Cross-site request");
		}
	}
}
