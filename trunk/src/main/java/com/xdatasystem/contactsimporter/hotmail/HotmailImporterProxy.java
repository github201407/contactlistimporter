package com.xdatasystem.contactsimporter.hotmail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import org.apache.http.HttpException;
import com.xdatasystem.contactsimporter.ContactListImporterException;
import com.xdatasystem.contactsimporter.WebClient;

/**
 * Hotmail importer that works with both new and the old hotmail version.
 * It multiplexes the methods to the correct implementation.
 * 
 * This implements the Strategy Design pattern.
 * The concrete importers (OldHotmailImporter, LiveHotmailImporter)
 * @author Tjerk Wolterink
 *
 */
public class HotmailImporterProxy extends AbstractHotmailImporter {
	private OldHotmailImporter oldImporter=null;
	private LiveHotmailImporter newImporter;
	private boolean isNewHotmailVersion=true;

	/**
	 * Constructs the proxy hotmail importer.
	 * 
	 * @param username the hotmail email adress of the user
	 * @param password the password of the user
	 */
	public HotmailImporterProxy(String username, String password) {
		super(username, password);
		// begin with new hotmail version, can always switch back (see login() method)
		newImporter=new LiveHotmailImporter(username, password);
		newImporter.setWebClient(getWebClient());
	}

	@Override
	public String getContactListURL() {
		if(isNewHotmailVersion) {
			return newImporter.getContactListURL();
		} else {
			return oldImporter.getContactListURL();
		}
	}

	@Override
	public String getLoginURL() {
		if(isNewHotmailVersion) {
			return newImporter.getLoginURL();
		} else {
			return oldImporter.getLoginURL();
		}
	}
	
	@Override
	protected void login(WebClient client) throws ContactListImporterException, IOException, URISyntaxException, InterruptedException, HttpException {
		newImporter.login(client);
    
		// now check the version
  	if(getWebClient().getCurrentUrl().contains("TodayLight.aspx")) {
  		// aha we're on the new page
  		isNewHotmailVersion=true;      
  		
  	} else {
  		// we have the old version
  		isNewHotmailVersion=false;      
  		oldImporter=new OldHotmailImporter(
  			this.getUsername(),
  			this.getPassword()
  		);
  		oldImporter.setWebClient(getWebClient());
  	}
	}
	
	protected InputStream getContactListContent(WebClient client, String listUrl, String referer) throws ContactListImporterException, URISyntaxException, InterruptedException, HttpException, IOException {
		if(isNewHotmailVersion) {
			return newImporter.getContactListContentLiveHotmail(client, listUrl, referer);
		} else {
			return super.getContactListContent(client, listUrl, referer);
		}
	}
		
}
