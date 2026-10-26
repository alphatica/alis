package com.alphatica.alis.studio.web;

import com.alphatica.alis.studio.web.jobs.JobRegistry;
import com.alphatica.alis.studio.web.live.LiveHub;
import io.javalin.Javalin;

import java.util.Set;

/**
 * REST API under /api, the WebSocket at /ws and the GUI for every other path.
 */
final class WebApp {

	private WebApp() {
	}

	static Javalin create(Set<String> allowedHosts) {
		return create(allowedHosts, () -> {
		});
	}

	/** {@code onRequest} runs before every request (used to close the splash screen once the browser connects). */
	static Javalin create(Set<String> allowedHosts, Runnable onRequest) {
		ErrorReports errorReports = new ErrorReports();
		JobRegistry jobs = new JobRegistry(errorReports);
		LiveHub hub = new LiveHub();
		LicenseGate licenseGate = new LicenseGate();
		return Javalin.create(config -> {
			config.startup.showJavalinBanner = false;
			config.jsonMapper(Json.JAVALIN);
			config.routes.before(ctx -> onRequest.run());
			Frontend.configure(config);
			new RequestGuard(allowedHosts).register(config.routes);
			ApiErrors.register(config.routes, errorReports);
			errorReports.register(config.routes);
			licenseGate.register(config.routes);
			AppApi.register(config.routes, licenseGate);
			new PortfolioApi(hub).register(config.routes);
			new DataApi(jobs, hub).register(config.routes);
			new SignalsApi().register(config.routes);
			new BacktestApi(jobs).register(config.routes);
			new OptimizationApi(jobs).register(config.routes);
			new BetterExitsApi(jobs).register(config.routes);
			jobs.register(config.routes, hub);
			hub.register(config.routes);
		});
	}
}
