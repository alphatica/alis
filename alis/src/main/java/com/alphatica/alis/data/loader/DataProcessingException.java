package com.alphatica.alis.data.loader;

public class DataProcessingException extends RuntimeException {
	private final Exception exception;

	public DataProcessingException(Exception exception) {
		this.exception = exception;
	}

	public Exception getException() {
		return exception;
	}
}
