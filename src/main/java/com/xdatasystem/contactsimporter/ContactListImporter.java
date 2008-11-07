package com.xdatasystem.contactsimporter;

//import java.io.IOException;
import java.util.List;
//import org.apache.http.HttpException;
import com.xdatasystem.user.Contact;

/**
 * Interface to the retrieval of contacts lists.
 * By using this interface we decouple the interface from
 * the actual implementation. 
 * 
 * @author Tjerk Wolterink
 */
public interface ContactListImporter {

	/**
	 * Gets the username ( often the emailadress) of the user
	 * for which contacts must be retrieved 
	 * 
	 * @return the username
	 */
	public String getUsername();
	
	/**
	 * Gets the password of the user for which
	 * the contacts must be retrieved.
	 * 
	 * @return the password of the user
	 */
	public String getPassword();
	
	/**
	 * Retrieves all contacts.
	 * This may take some time because first the user
	 * must be logged in, and then the contact list must 
	 * be retrieved.
	 * <br/>
	 * Only contacts that have an email adress should be retrieved,
	 * however it is good practice to actually check for an
	 * email adress in a contact.
	 * 
	 * @return a list of contacts
	 * @throws ContactListImporterException
	 */
	public abstract List<Contact> getContactList() throws ContactListImporterException;
}
