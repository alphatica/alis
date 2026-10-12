package com.alphatica.alis.studio.web;

import io.javalin.config.RoutesConfig;
import io.javalin.http.HttpStatus;

/**
 * Errors are returned as {@link ErrorMessage} JSON and shown by the GUI as toasts.
 * Unexpected errors are reported only when the user clicks Report.
 */
final class ApiErrors {

	private ApiErrors() {
	}

	static void register(RoutesConfig routes, ErrorReports errorReports) {
		routes.exception(ValidationException.class, (e, ctx) ->
				ctx.status(HttpStatus.UNPROCESSABLE_CONTENT).json(ErrorMessage.invalidInput(e.getMessage())));
		routes.exception(Exception.class, (e, ctx) ->
				ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(ErrorMessage.unexpected(errorReports, e)));
	}
}
