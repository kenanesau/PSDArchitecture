/**
 * 
 */
package com.privatesecuredata.arch.exceptions;

/**
 * @author kenan
 *
 */
public class ArgumentException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public ArgumentException() {
	}

	/**
	 * @param detailMessage
	 */
	public ArgumentException(String detailMessage) {
		super(detailMessage);
	}

	/**
	 * @param throwable
	 */
	public ArgumentException(Throwable throwable) {
		super(throwable);
	}

	/**
	 * @param detailMessage
	 * @param throwable
	 */
	public ArgumentException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
