package com.alphatica.alis.studio.web;

import java.util.Objects;

/**
 * Error shown to the user as a toast. Unexpected errors have a report id, so the user can choose to report them.
 *
 * @param reportId null for errors caused by the user
 */
public record ErrorMessage(String title, String message, String reportId) {

	public static ErrorMessage invalidInput(String message) {
		return new ErrorMessage("Invalid input", message, null);
	}

	public static ErrorMessage unexpected(ErrorReports errorReports, Exception exception) {
		String message = Objects.toString(exception.getMessage(), exception.toString());
		return new ErrorMessage("Unexpected error", message, errorReports.store(message, exception));
	}
}
