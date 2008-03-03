package com.xdatasystem.contactsimporter;

public class AuthenticationException extends ContactListImporterException {
	
	AuthenticationException(String message) {
		super(message);
	}
	
	AuthenticationException(String message, Throwable t) {
		super(message, t);
	}
}
