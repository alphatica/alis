package com.alphatica.alis.data.loader;

public class DataProcessingException extends RuntimeException {
	public enum Reason {
		PROCESSING_FAILED,
		NO_DATA,
		DATA_NOT_FOUND,
		LOAD_FAILED,
		UNZIP_FAILED
	}

	private final Reason reason;
	private final Exception exception;

	public DataProcessingException(Exception exception) {
		this(Reason.PROCESSING_FAILED, exception);
	}

	public DataProcessingException(Reason reason) {
		this(reason, null);
	}

	public DataProcessingException(Reason reason, Exception exception) {
		super(exception);
		this.reason = reason;
		this.exception = exception;
	}

	public Reason getReason() {
		return reason;
	}

	public Exception getException() {
		return exception;
	}
}
