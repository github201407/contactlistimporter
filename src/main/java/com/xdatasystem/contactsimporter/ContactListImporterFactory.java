package com.xdatasystem.contactsimporter;

import com.xdatasystem.contactsimporter.gmail.GmailImporter;
import com.xdatasystem.contactsimporter.hotmail.AbstractHotmailImporter;
import com.xdatasystem.contactsimporter.hotmail.HotmailImporterProxy;
import com.xdatasystem.contactsimporter.hotmail.LiveHotmailImporter;
import com.xdatasystem.contactsimporter.hyves.HyvesImporter;
import com.xdatasystem.contactsimporter.yahoo.YahooImporter;

/**
 * Factory for creating ContactListImporter objects.
 * Implemented using the factory pattern.
 * 
 * @author Tjerk Wolterink
 */
public class ContactListImporterFactory {

	/**
	 * Guesses which service to use given an input email adress.
	 * Note that this does not work for all e-mail adresses,
	 * gmail for example allow users to use their own domain name,
	 * then the guess will fail.
	 * Returns null if no match could be found
	 * 
	 * @param email
	 * @param password
	 * @return a importer for the email adress or null if no importer could be found
	 */
	public static ContactListImporter guess(String email, String password) {
		if(AbstractHotmailImporter.isHotmail(email)) {
			return new HotmailImporterProxy(email, password);
		
		} else if(GmailImporter.isGmail(email)) {
			return new GmailImporter(email, password);
			
		} else if(YahooImporter.isYahoo(email)) {
			return new YahooImporter(email, password);
			
		}
		return null;
	}
	
	public static ContactListImporter hotmail(String email, String password) {
		return new LiveHotmailImporter(email, password);
	}
	
	public static ContactListImporter gmail(String email, String password) {
		return new GmailImporter(email, password);
	}
	
	public static ContactListImporter hyves(String email, String password) {
		return new HyvesImporter(email, password);
	}
		
	public static ContactListImporter yahoo(String email, String password) {
		return new YahooImporter(email, password);
	}
}
