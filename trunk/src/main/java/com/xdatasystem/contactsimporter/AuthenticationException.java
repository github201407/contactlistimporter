package com.xdatasystem.contactsimporter;

/**
 * When the user credentials (loginname, password) are incorrect when trying to login,
 * the this exception is thrown.
 * 
 * @author Tjerk Wolterink
 */
public class AuthenticationException extends ContactListImporterException {
	
	public AuthenticationException(String message) {
		super(message);
	}
	
	public AuthenticationException(String message, Throwable t) {
		super(message, t);
	}
}
