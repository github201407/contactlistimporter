package com.xdatasystem.contactsimporter;

import com.xdatasystem.contactsimporter.hotmail.HotmailImporter;

/**
 * Factory for creating ContactListImporter objects.
 * Implemented using the factory pattern.
 * 
 * @author Tjerk Wolterink
 */
public class ContactListImporterFactory {

	public static ContactListImporter create(String email, String password) {
		return new HotmailImporter(email, password);
	}
}
