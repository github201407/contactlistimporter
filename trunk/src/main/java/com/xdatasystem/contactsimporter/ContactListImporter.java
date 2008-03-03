package com.xdatasystem.contactsimporter;

import java.io.IOException;
import java.util.List;

/**
 * Interface to the retrieval of contacts lists.
 * By using this interface we decouple the interface from
 * the actual implementation. 
 * 
 * @author Tjerk Wolterink
 */
public interface ContactListImporter {

	public abstract List<Contact> getContactList() throws IOException, ContactListImporterException;;
}
