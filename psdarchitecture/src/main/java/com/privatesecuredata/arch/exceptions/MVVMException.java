/**
 * 
 */
package com.privatesecuredata.arch.exceptions;

/**
 * @author kenan
 *
 */
public class MVVMException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 */
	public MVVMException() {}

	/**
	 * @param detailMessage
	 */
	public MVVMException(String detailMessage) {
		super(detailMessage);
	}

	/**
	 * @param throwable
	 */
	public MVVMException(Throwable throwable) {
		super(throwable);
	}

	/**
	 * @param detailMessage
	 * @param throwable
	 */
	public MVVMException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

}
