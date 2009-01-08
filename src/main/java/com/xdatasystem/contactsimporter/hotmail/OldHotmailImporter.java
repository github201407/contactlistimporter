package com.xdatasystem.contactsimporter.hotmail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpException;
import org.apache.http.NameValuePair;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import com.xdatasystem.contactsimporter.*;
import com.xdatasystem.user.Contact;
//import java.util.logging.Logger;
import java.util.regex.*;

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
