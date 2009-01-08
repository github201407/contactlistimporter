package com.xdatasystem.contactsimporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.apache.http.HttpException;
import com.xdatasystem.user.Contact;

/**
 * Abstract, general implementation of ContactListImporter.
 * It provides usefull methods when importing contacts from a service.
 * Subclass this class to implement a service specific contactsimporter.
 * 
 * @author Tjerk Wolterink
 */
public abstract class ContactListImporterImpl implements ContactListImporter {
	private String username;
	private String password;
	private static Logger log = Logger.getLogger(ContactListImporterImpl.class.getName());
	private Pattern emailPattern;
	private WebClient client;

	public ContactListImporterImpl(String username, String password) {
		this.username=username;
		this.password=password;
		emailPattern=Pattern.compile(
			"^[0-9a-z]([-_.~]?[0-9a-z])*@[0-9a-z]([-.]?[0-9a-z])*\\.[a-z]{2,4}$"
		);
		client=new WebClient();
	}
	
	protected Logger getLogger() {
		return log;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public abstract String getLoginURL();
	
	public abstract String getContactListURL();
	
	public boolean isEmailAddress(String email) {
		return emailPattern.matcher(email).matches();
	}

	public WebClient getWebClient() {
		return client;
	}
	
	public void setWebClient(WebClient client) {
		this.client=client;
	}
	
	public List<Contact> getContactList() throws ContactListImporterException {

		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
		//System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "debug");
		
		try {
			
			log.info("Performing login");
			login(client);
			log.info("Login succeeded");
			
	   	String host=client.getHostName();
	   	
			return this.getAndParseContacts(client, host);
	    
		} catch(Exception e) {
			if(e instanceof ContactListImporterException) {
				throw (ContactListImporterException)e;
			}
			throw new ContactListImporterException("Exception occured: "+e.getMessage(), e);
		}
	}
	
	protected List<Contact> getAndParseContacts(WebClient client, String host) throws Exception {
		
		String listUrl=String.format(getContactListURL(), host);
		log.info("Retrieving contactlist");
		InputStream input=this.getContactListContent(client, listUrl, null);
		log.info("Parsing contactlist");
    List<Contact> contacts=parseContacts(input);
    // make sure the stream is closed
    input.close();
    return contacts;
	}
	
	/**
	 * A subclass should run this method if something unexpected
	 * happend: The service probably changed protocols. 
	 * @throws ContactListImporterException
	 */
	protected void throwProtocolChanged() throws ContactListImporterException {
		throw new ContactListImporterException(
			"The email service '"+this.getClass().getName()+
			"' changed it's protocol, cannot import contactslist"
		);
	}

	/**
	 * Gets the contact list using HTTP Get,
	 * override to implement your own implementation
	 * 
	 * @return the content of the contact list as an inputstream
	 */
	protected InputStream getContactListContent(WebClient client, String listUrl, String referer)
		throws ContactListImporterException, URISyntaxException, InterruptedException, HttpException, IOException
	{
		return client.doGet(listUrl, referer);
	}

	/**
	 * Performs the login. The client is logged in after this method call.
	 * 
	 * @return the current host location url.
	 */
	protected abstract void login(WebClient client)
	throws Exception;
	
	/**
	 * Parses the contactContent string that was retrieved and
	 * returns the contacts as a list
	 * 
	 * @para, contactsContent the content of the contacts file retrieved from the server
	 * @return a list of contacts parsed from the contactsContent
	 */
	protected abstract List<Contact> parseContacts(InputStream contactsContent) throws Exception;
	
	/**
	 * Reads an inputstream and converts it to a string.
	 * Note that this is rather memory intensive, if you
	 * do not need random access in the inputstream you
	 * should iterate sequentially over the lines using
	 * readLine()
	 * 
	 * @param is the inputstream to convert
	 * @return the content of the input stream
	 * @throws IOException if reading the inputstream fails
	 */
	protected String readInputStream(InputStream is) throws IOException {
		BufferedReader in=new BufferedReader(new InputStreamReader(is, "UTF-8"));
		StringBuffer buffer=new StringBuffer();
		String line;
		while ((line = in.readLine()) != null) {
			buffer.append(line);
		}
		is.close();
		return buffer.toString();
	}
	
	/**
	 * Reads an inputstream so that it is consumed
	 * 
	 * @param is the inputstream to convert
	 * @throws IOException if reading the inputstream fails
	 */
	protected void consumeInputStream(InputStream is) throws IOException {
		BufferedReader in=new BufferedReader(new InputStreamReader(is));

		while (in.readLine() != null) {
		}
		is.close();
	}
	
	/**
	 * Wether the email ends with one of the domains
	 * in the domains list.
	 * 
	 * @param email the email tot test
	 * @param domains a list of domains. 
	 * @return true if the email ends with one of the domains in the domains list
	 */
	public static boolean isConformingEmail(String email, String[] domains) {
		if(email==null) return false;
		for(String d : domains) {
			if(email.indexOf(d)==email.length()-d.length()) {
				return true;
			}
		}
		return false;
	}

}
