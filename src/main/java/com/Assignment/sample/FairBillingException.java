package com.Assignment.sample;

@SuppressWarnings("serial")
public class FairBillingException extends RuntimeException {

	public FairBillingException() {
		super();
	}
	
	public FairBillingException(Exception e) {
		super(e);
	}

	public FairBillingException(String message, Exception e) {
		super(message, e);
	}
}
