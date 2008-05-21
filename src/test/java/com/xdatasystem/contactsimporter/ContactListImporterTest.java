package com.xdatasystem.contactsimporter;

import java.util.List;

import com.xdatasystem.user.Contact;

import junit.framework.TestCase;

public class ContactListImporterTest extends TestCase {

	public void testHotmailImporter() throws Exception {
		ContactListImporter importer=ContactListImporterFactory.guess("tjerkwolterink@hotmail.com", "PASS");
		testImporter(importer);
	}

	public void testGmailImporter() throws Exception {
		ContactListImporter importer=ContactListImporterFactory.guess("tjerkw@gmail.com", "PASS");
		testImporter(importer);
	}

	public void testYahooImporter() throws Exception {
		ContactListImporter importer=ContactListImporterFactory.guess("tjerkw@yahoo.com", "PASS");
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
