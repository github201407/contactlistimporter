package com.xdatasystem.contactsimporter;

/**
 * Represents a generic exception that occurs when
 * retrieving contacts. This exception often
 * wraps another exception like an IOException.
 * 
 * @author Tjerk Wolterink
 */
public class ContactListImporterException extends Exception {

	public ContactListImporterException(String message) {
		super(message);
	}
	
	public ContactListImporterException(String message, Throwable t) {
		super(message, t);
	}
}
