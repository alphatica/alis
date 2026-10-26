package com.alphatica.alis.studio.logic.options;

/** The market data lacks what a scorer needs; the message is for the user. */
public class UnavailableScorerException extends Exception {

	public UnavailableScorerException(String message) {
		super(message);
	}
}
