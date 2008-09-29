package com.xdatasystem.contactsimporter;

import java.util.List;
import com.xdatasystem.user.Contact;
import junit.framework.TestCase;

public class ContactListImporterTest extends TestCase {

	// Note: the passwords are wrong to protect my mail boxes,
	// fill in correct credentials to test it
	public void testHotmailImporter() throws Exception {
		ContactListImporter importer=ContactListImporterFactory.guess("tjerkwolterink@hotmail.com", "password");
		testImporter(importer);
	}

	public void testGmailImporter() throws Exception {
		ContactListImporter importer=ContactListImporterFactory.guess("tjerkw@gmail.com", "yyy");
		testImporter(importer);
	}
	
	/* Hyves changed protocols.. does not work
	public void testHyvesImporter() throws Exception {
		//ContactListImporter importer=ContactListImporterFactory.hyves("testusertest", "testuser");
		ContactListImporter importer=ContactListImporterFactory.hyves("tjerkwolterink", "yyy");
		testImporter(importer);
	}*/

	
	public void testYahooImporter() throws Exception {
		ContactListImporter importer=ContactListImporterFactory.guess("tjerkwolterink@yahoo.com", "password");
		testImporter(importer);
	}
	

	public void testImporter(ContactListImporter importer) throws ContactListImporterException, InterruptedException {
		System.out.println("Testing importer: "+importer.getClass().getName());
		List<Contact> contacts=importer.getContactList();
		for(Contact c : contacts) {
			System.out.println(c);
		}
		System.out.println("Number of contacts found: "+contacts.size());
		Thread.currentThread().sleep(2000);
	}
}
