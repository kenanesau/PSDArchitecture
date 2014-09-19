package com.privatesecuredata.arch.exceptions;

public class CommitException extends RuntimeException {

	public CommitException() {
		super();
	}

	public CommitException(String detailMessage, Throwable throwable) {
		super(detailMessage, throwable);
	}

	public CommitException(String detailMessage) {
		super(detailMessage);
	}

	public CommitException(Throwable throwable) {
		super(throwable);
	}
	
}
