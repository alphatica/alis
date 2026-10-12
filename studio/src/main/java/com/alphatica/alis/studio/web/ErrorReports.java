package com.alphatica.alis.studio.web;

import io.javalin.config.RoutesConfig;
import io.javalin.http.HttpStatus;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps recent unexpected errors, so the user can choose to report them.
 */
public final class ErrorReports {
	private static final int MAX_PENDING = 50;

	private final Map<String, PendingError> pending = Collections.synchronizedMap(new LinkedHashMap<>() {
		@Override
		protected boolean removeEldestEntry(Map.Entry<String, PendingError> eldest) {
			return size() > MAX_PENDING;
		}
	});

	/**
	 * @return id to be used in the report button
	 */
	public String store(String message, Exception exception) {
		String id = UUID.randomUUID().toString();
		pending.put(id, new PendingError(message, exception));
		return id;
	}

	void register(RoutesConfig routes) {
		routes.post("/api/errors/{id}/report", ctx -> {
			PendingError error = pending.remove(ctx.pathParam("id"));
			if (error != null) {
				ErrorReporter.reportError(error.message(), error.exception());
			}
			ctx.status(HttpStatus.NO_CONTENT);
		});
	}

	private record PendingError(String message, Exception exception) {
	}
}
