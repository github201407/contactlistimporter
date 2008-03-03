package com.xdatasystem.contactsimporter;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

public abstract class ContactListImporterImpl implements ContactListImporter {
	private String username;
	private String password;

	public ContactListImporterImpl(String username, String password) {
		this.username=username;
		this.password=password;
	}
	
	protected String getUsername() {
		return username;
	}
	
	protected String getPassword() {
		return password;
	}
	
	public abstract URL getLoginURL();
	public abstract URL getContactListURL();
	
	public abstract List<Contact> getContactList() throws IOException, ContactListImporterException;

}
