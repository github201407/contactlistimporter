package com.xdatasystem.contactsimporter;

import java.util.List;
import junit.framework.TestCase;

public class ContactListImporterTest extends TestCase {
	
	public void testHotmailImporter() throws Exception {
		ContactListImporter importer=ContactListImporterFactory.create("tjerkwolterink@hotmail.com", "jowjow");
		List<Contact> contacts=importer.getContactList();
		for(Contact c : contacts) {
			System.out.println(c);
		}
	}
}
