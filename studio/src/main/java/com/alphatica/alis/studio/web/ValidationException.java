package com.alphatica.alis.studio.web;

/**
 * Error caused by user input. Shown to the user, not reported.
 */
public class ValidationException extends RuntimeException {

	public ValidationException(String message) {
		super(message);
	}
}
