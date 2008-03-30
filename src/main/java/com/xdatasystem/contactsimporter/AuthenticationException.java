package com.xdatasystem.contactsimporter;

public class AuthenticationException extends ContactListImporterException {
	
	public AuthenticationException(String message) {
		super(message);
	}
	
	public AuthenticationException(String message, Throwable t) {
		super(message, t);
	}
}
