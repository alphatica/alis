package com.alphatica.alis.studio.dao;

public class DaoException extends Exception {
	public DaoException(String s, Throwable throwable) {
		super(s);
		initCause(throwable);
	}
}
