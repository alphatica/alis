package com.alphatica.alis.studio.web;

import com.alphatica.alis.studio.license.LicenseAcceptance;
import com.alphatica.alis.studio.license.LicenseText;
import com.alphatica.alis.studio.web.live.LiveHub;
import io.javalin.config.RoutesConfig;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HttpStatus;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The API and the WebSocket are closed until the user accepts the license; the GUI shows the license first.
 */
final class LicenseGate {
	private static final String LICENSE_PATH = "/api/license";
	private static final List<String> OPEN_PATHS = List.of(AppApi.PATH, LICENSE_PATH, "/api/errors/");

	private final AtomicBoolean accepted = new AtomicBoolean(readAcceptance());

	boolean isAccepted() {
		return accepted.get();
	}

	void register(RoutesConfig routes) {
		routes.before("/api/*", this::rejectUntilAccepted);
		routes.wsBeforeUpgrade(LiveHub.PATH, ctx -> {
			if (!accepted.get()) {
				throw new ForbiddenResponse("License not accepted");
			}
		});
		routes.get(LICENSE_PATH, ctx -> ctx.json(Map.of("text", LicenseText.read())));
		routes.post(LICENSE_PATH + "/accept", this::accept);
		routes.post(LICENSE_PATH + "/decline", ctx -> {
			ctx.status(HttpStatus.NO_CONTENT);
			Shutdown.afterResponse();
		});
	}

	private void rejectUntilAccepted(Context ctx) {
		if (accepted.get() || OPEN_PATHS.stream().anyMatch(ctx.path()::startsWith)) {
			return;
		}
		ctx.status(HttpStatus.FORBIDDEN).json(new ErrorMessage("License", "Accept the license first.", null));
		ctx.skipRemainingHandlers();
	}

	private void accept(Context ctx) throws IOException {
		LicenseAcceptance.accept();
		accepted.set(true);
		ctx.status(HttpStatus.NO_CONTENT);
	}

	private static boolean readAcceptance() {
		try {
			return LicenseAcceptance.isAccepted();
		} catch (IOException e) {
			System.err.println("Unable to read license acceptance: " + e);
			return false;
		}
	}
}
