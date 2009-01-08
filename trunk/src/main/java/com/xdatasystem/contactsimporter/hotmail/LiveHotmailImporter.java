package com.xdatasystem.contactsimporter.hotmail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpException;
import org.apache.http.NameValuePair;
import org.apache.http.cookie.Cookie;
import org.apache.http.message.BasicNameValuePair;
import com.xdatasystem.contactsimporter.ContactListImporterException;
import com.xdatasystem.contactsimporter.WebClient;

/**
 * Implementation of ContactListImporter that retrieves contacts
 * from microsoft's hotmail service.
 * This class only works with the new hotmail version: Hotmail Live.
 * Use the HotmailImporterProxy, the proxy chooses the correct importer.
 * 
 * @author Thomas Bernard
 * @author Tjerk Wolterink
 */
public class LiveHotmailImporter extends AbstractHotmailImporter {
	
	/**
	 * Constructs a LiveHotmailImpoter for a specific user.
	 * We recommend usage of the ContactListImporterFactory factory object.
	 * This decouples instantiation from implementation, and follows
	 * the Factory Software Pattern.
	 * 
	 * @param username the hotmail email address of the user
	 * @param password the password of the user
	 */
	public LiveHotmailImporter(String username, String password) {
		super(username, password);
	}

	@Override
	public String getContactListURL() {
		return "http://%s/mail/options.aspx?subsection=26";
	}

	@Override
	protected void login(WebClient client) throws ContactListImporterException, IOException, URISyntaxException, InterruptedException, HttpException {
		InputStream is=super.loginAndRedirect(client);

		// The change from the old to the new hotmail
  	// means that there may exist a message that indicates this change.
  	// This message must be dealt with. It is also used to know 
  	// if we have the old or the new hotmail version
  	//
    dissmissMessageAtLogin(client, this.readInputStream(is));
	}

	/**
	 * Dismisses the message at login (if any)
	 *
	 * @return true if a message was found and dimissed, false otherwise
	 */
	private boolean dissmissMessageAtLogin(WebClient client, String content) throws ContactListImporterException, HttpException, IOException, InterruptedException, URISyntaxException {
		String ref=client.getCurrentUrl();
		String messageFormName="MessageAtLoginForm";
		
    if(!content.contains(messageFormName)) return false;
  	// a message is shown that the account of the user is updated to the new version,
  	// dismiss the message:
  	int index=content.indexOf(messageFormName)+messageFormName.length();
  	Pattern p=Pattern.compile("^.*action=\"([^\\s\"]+)\"");
		String action=content.substring(index, index+200 > content.length() ? content.length() : index+200);
		
		//System.out.println(action);
		
		Matcher matcher=p.matcher(action);
		if(!matcher.find()) {
    	// no post
			this.throwProtocolChanged();
    }
    action=matcher.group(1);
    if(action.indexOf("http")!=0) {
    	action=ref.substring(0, ref.lastIndexOf("/")+1)+action;
    }
    //System.out.println("action found: "+action);
    
    String viewState = getInputValue("__VIEWSTATE", content);
		String eventValidation = getInputValue("__EVENTVALIDATION", content);
		
    NameValuePair[] data = {
			new BasicNameValuePair("__VIEWSTATE", viewState),
			new BasicNameValuePair("__EVENTVALIDATION", eventValidation),
			new BasicNameValuePair("DontShowMessageAtLoginCheckbox", "1"),
			new BasicNameValuePair("TakeMeToInbox", "Continue")
		};
    InputStream is=client.doPost(action, data, ref);
    super.consumeInputStream(is);
  	
    return true;
    // dismiss done;
	}
	
	/**
	 * Simulate going to the Export Contacts page and then clicking the export button.
	 * 
	 * @author Thomas Bernard
	 */
	protected InputStream getContactListContent(WebClient client, String listUrl, String referer) throws ContactListImporterException, URISyntaxException, InterruptedException, HttpException, IOException {
		return getContactListContentLiveHotmail(client, listUrl, referer);
	}
	
	InputStream getContactListContentLiveHotmail(WebClient client, String listUrl, String referer) throws ContactListImporterException, URISyntaxException, InterruptedException, HttpException, IOException {
		InputStream page = client.doGet(listUrl, referer);
		BufferedReader blop = new BufferedReader(new InputStreamReader(page, "CP1252")); // we could have the charset from the entity
		// get values for form :
		// <input type="hidden" name="__VIEWSTATE" id="__VIEWSTATE" value="/wEPDwULLTE5Mzg1OTc0NjQPZBYCAgkPZBYCZg9kFgJmD2QWAmYPDxYCHgRUZXh0BRVFeHBvcnRlciBkZXMgY29udGFjdHNkZGRhOT6nOnCRJvBE38tzct7mj9mv4g==" />
		Pattern pViewState = Pattern.compile(".*name=\"__VIEWSTATE\".*value=\"([^\"]*)\".*");
		String viewState = null;
		// <input type="hidden" name="__EVENTVALIDATION" id="__EVENTVALIDATION" value="/wEWAgKLx4TKCALy4eKoCKyMGoHSihkXecaJI7cozQvjMqGz" />
		Pattern pEventValidation = Pattern.compile(".*name=\"__EVENTVALIDATION\".*value=\"([^\"]*)\".*");
		String eventValidation = null;
		String line = blop.readLine();
		while(viewState == null || eventValidation == null) {
			line = blop.readLine();
			
			if(line == null) break;
			
			Matcher mViewState = pViewState.matcher(line);
			if(mViewState.matches()) {
				viewState = mViewState.group(1);
			} else {
				Matcher mEventValidation = pEventValidation.matcher(line);
				if(mEventValidation.matches()) {
					eventValidation = mEventValidation.group(1);
				}
			}
		}
		blop.close();
		
		if(viewState == null || eventValidation == null) {
			// NOT GOOD !
			throw new ContactListImporterException("Could not fill form for getting CSV from Hotmail");
		}
		String mt = "";
		for(Cookie cookie : client.getHttpClient().getCookieStore().getCookies()) {
			if(cookie.getName().equals("mt")) {
				mt = cookie.getValue();
			}
		}
		NameValuePair[] data = {
				new BasicNameValuePair("__VIEWSTATE", viewState),
				new BasicNameValuePair("__EVENTVALIDATION", eventValidation),
				new BasicNameValuePair("ctl02$ExportButton", "Exporter des contacts"),
				new BasicNameValuePair("mt", mt)
		};
		return client.doPost(listUrl, data, listUrl);
	}
}
