package com.xdatasystem.contactsimporter.hotmail;

/**
 * Implementation of ContactListImporter that retrieves contacts
 * from microsoft's hotmail service.
 * 
 * Note: This implementation works only for the
 * old hotmail version.
 * 
 * Use the HotmailImporterProxy, the proxy chooses the correct importer.
 * 
 * @author Tjerk Wolterink
 */
public class OldHotmailImporter extends AbstractHotmailImporter {
	
	/**
	 * Constructs a HotmailImporter for a specific user.
	 * We recommend usage of the ContactListImporterFactory factory object.
	 * This decouples instantiation from implementation, and follows
	 * the Factory Software Pattern.
	 * 
	 * @param username the hotmail email address of the user
	 * @param password
	 */
	public OldHotmailImporter(String username, String password) {
		super(username, password);
	}

	@Override
	public String getContactListURL() {
		return "http://%s/mail/GetContacts.aspx";
	}
}
