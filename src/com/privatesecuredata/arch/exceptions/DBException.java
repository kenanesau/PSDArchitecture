package com.privatesecuredata.arch.exceptions;

public class DBException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public DBException() {}

	/**
	 * @param detailMessage
	 */
	public DBException(String detailMessage) {
		super(detailMessage);
	}

	/**
	 * @param throwable
	 */
	public DBException(Throwable throwable) {
		super(throwable);
	}

	/**
	 * @param detailMessage
	 * @param throwable
	 */
	public DBException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
