package com.alphatica.alis.studio.web;

import com.alphatica.alis.studio.state.AppState;
import io.javalin.config.RoutesConfig;
import io.javalin.http.HttpStatus;

import static com.alphatica.alis.studio.Constants.STUDIO_NAME;

/**
 * State the GUI needs at start, and quitting the application (closing the browser tab does not stop the JVM).
 */
final class AppApi {
	static final String PATH = "/api/app";

	private AppApi() {
	}

	static void register(RoutesConfig routes, LicenseGate licenseGate) {
		routes.get(PATH, ctx -> ctx.json(new AppStatus(licenseGate.isAccepted(), AppState.getDataStatus(), STUDIO_NAME)));
		routes.post("/api/shutdown", ctx -> {
			ctx.status(HttpStatus.NO_CONTENT);
			Shutdown.afterResponse();
		});
	}

	record AppStatus(boolean licenseAccepted, String dataStatus, String studioName) {
	}
}
