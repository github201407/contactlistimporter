package com.xdatasystem.contactsimporter.hotmail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpException;
import org.apache.http.NameValuePair;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import com.xdatasystem.contactsimporter.AuthenticationException;
import com.xdatasystem.contactsimporter.ContactImpl;
import com.xdatasystem.contactsimporter.ContactListImporterException;
import com.xdatasystem.contactsimporter.ContactListImporterImpl;
import com.xdatasystem.contactsimporter.MemorizingRedirectHandler;
import com.xdatasystem.user.Contact;

/**
 * Implementation of ContactListImporter that retrieves contacts
 * from microsoft's hotmail service.
 * 
 * @author Tjerk Wolterink
 * @author Thomas Bernard
 */
public class HotmailImporter extends ContactListImporterImpl {
	private final static String PWDPAD="IfYouAreReadingThisYouHaveTooMuchFreeTime";
	private boolean isNewHotmailVersion=false;
	
	/**
	 * Constructs a HotmailImpoter for a specific user.
	 * We recommand usage of the ContactListImporterFactory factory object.
	 * This decouples instantiation from implementation, and follows
	 * the Factory Software Pattern.
	 * 
	 * @param username the hotmail email adress of the user
	 * @param password
	 */
	public HotmailImporter(String username, String password) {
		super(username, password);
	}

	@Override
	public String getLoginURL() {
		return "http://login.live.com/login.srf?id=2";
	}

	@Override
	public String getContactListURL() {
		return isNewHotmailVersion ? 
			// new version url
			"http://%s/mail/options.aspx?subsection=26"
			:
			// old version url
			"http://%s/mail/GetContacts.aspx"
		;
	}

	@Override
	protected void login(DefaultHttpClient client) throws ContactListImporterException, IOException, URISyntaxException, InterruptedException, HttpException {
		String loginPageUrl=getLoginURL().toString();
		getLogger().info("Requesting login page");
		String content=this.readInputStream(
			this.doGet(client, loginPageUrl, null)
		);
		
    String ppsx=getInputValue("PPSX", content);
		String ppft=getInputValue("PPFT", content);
		String formUrl=getFormUrl(content);
		//formUrl="https://login.live.com/ppsecure/post.srf";
		
		// security cookie
		BasicClientCookie cookie=new BasicClientCookie("CkTst", "G"+System.currentTimeMillis());
		cookie.setDomain("login.live.com");
		cookie.setPath("/ppsecure/");
		client.getCookieStore().addCookie(cookie);
		
		// stupid microsoft security haha
		String pwdPad=PWDPAD.substring(0, PWDPAD.length()-this.getPassword().length());
		
		NameValuePair[] data = {
			new BasicNameValuePair("PPSX", ppsx),
			new BasicNameValuePair("PwdPad", pwdPad),
			new BasicNameValuePair("login", this.getUsername()),
			new BasicNameValuePair("passwd", this.getPassword()),
			new BasicNameValuePair("LoginOptions", "2"),
			new BasicNameValuePair("PPFT", ppft)
		};
		
		getLogger().info("Performing login");
		content=this.readInputStream(
			this.doPost(client, formUrl, data, loginPageUrl)
		);
		
    if(content.contains("password is incorrect")) {
    	getLogger().info("Login failed, username and password do not match");
	    throw new AuthenticationException("Username and password do not match");
    }	
    
    if(content.contains("type your e-mail address in the following format")) {
    	getLogger().info("Login failed, username not a valid email adress");
	    throw new AuthenticationException("Username must be in the following format  yourname@example.com (hotmail.com)");
    }
    
    
    String redirectLocation=getJSRedirectLocation(content);
    getLogger().info("Redirecting to " + redirectLocation);
    InputStream is=this.doGet(client, redirectLocation, formUrl);
    
  	// The change from the old to the new hotmail
  	// means that there may exist a message that indicates this change.
  	// This message must be dealt with. It is also used to know 
  	// if we have the old or the new hotmail version
  	//
    dissmissMessageAtLogin(client, this.readInputStream(is));
    
    // now check the version
  	if(getCurrentUrl().contains("TodayLight.aspx")) {
  		// aha we're on the new page
  		isNewHotmailVersion=true;        
  	}
	}

	/**
	 * Dismises the message at login (if any)
	 *
	 * @return true if a message was found and dimissed, false otherwise
	 */
	private boolean dissmissMessageAtLogin(DefaultHttpClient client, String content) throws ContactListImporterException, HttpException, IOException, InterruptedException, URISyntaxException {
		String ref=((MemorizingRedirectHandler)client.getRedirectHandler()).getLastLocation().toString();
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
    InputStream is=this.doPost(client, action, data, ref);
    super.consumeInputStream(is);
  	
    return true;
    // dismiss done;
	}

	/**
	 * Read the redirection location from Javascript code.
	 * @param content HTML page
	 * @return the url where the browser would be redirected
	 * @throws ContactListImporterException
	 */
	private String getJSRedirectLocation(String content) throws ContactListImporterException {
		//window.location.replace("http://www.hotmail.msn.com/cgi-bin/sbox?t=90Z!bPVpcHQfl1mtmDsItcDs0CTVpH4WzaBDPYcvc8RVXXH9L2aVWsXmDTlOH4ydC5qVTYVFsP7ezznTp512N6H0cc1yZuQ6bzyqoieqxOIq4zRIudn84A8BIxCKwVQ!WEqpLyWu4KK4o$&p=9ydC!8tdqCZERmTtuXT7jRHP0wZ8AdvQ0oUpqtI1BqG!KHe0JPjnMzttVhgwZj9UQllJozZ4JIKQh!yTym6QoWrzUZZD2G4MptwTBRaBQcN0LRYJfawvO7fccjMe4HbNsQowgAdpJbPKNjb!q0jG2QVTOsXrGlPyGi1cutfy0ToMZdThLo63SDm2388NJL!YWnBGN4bUVTJ!0$&lc=1033&id=2")
		String name="window.location.replace(\"";
		int index=content.indexOf(name)+name.length();
		if(index==-1) {
			throwProtocolChanged();
		}
		content=content.substring(index);
		content=content.substring(0, content.indexOf("\""));
		return content;
	}

	@Override
	protected List<Contact> parseContacts(InputStream contactsContent) throws IOException {
		List<Contact> contacts=new ArrayList<Contact>(10);
		/* Hotmail sends a file in charset "Windows CP 1252"
		 * separated with ';' and with CRLF line termination. */
		BufferedReader in=new BufferedReader(new InputStreamReader(contactsContent, "CP1252"));
		String separator=";";
		String line;
		int i=0;
		while ((line = in.readLine()) != null) {
			//System.out.println(line);
			if(i>0) {
				if(i==1 && !line.contains(separator)) {
					separator=",";
				}
				String[] values=line.split(separator);
				if(values.length<47) continue;
				String email=parseValue(values[46]);
				int atIndex=email.indexOf("@");
				// only add real contacts with a email adress 
				if(email.length()==0 || atIndex==-1) continue;
				
				
				String name=parseValue(values[1]);
				if(values[2].length()>0)
					name+=" "+parseValue(values[2]);
				if(values[3].length()>0)
					name+=" "+parseValue(values[3]);
				if(name.length()==2) name=email.substring(0, atIndex);
				
				email=email.toLowerCase();
				
				if(isEmailAddress(email)) {
					contacts.add(new ContactImpl(name, email));
				}
			}
			i++;
		}
		return contacts;
	}
	
	private String parseValue(String value) {
		// chop off quotes
		if(value.length()>0 && value.charAt(0)=='"') {
			value=value.substring(1, value.length()-1);
		}
		return value;
	}

	/**
	 * Get a value from a HTML form
	 * @param name the name of the input element
	 * @param content the HTML document
	 * @return the value of the input element
	 * @throws ContactListImporterException
	 */
	private String getInputValue(String name, String content) throws ContactListImporterException {
		Pattern p=Pattern.compile("^.*value=\"([^\\s\"]+)\"");
		int index=content.indexOf(name)+name.length()+2;
		content=content.substring(index, index+200 > content.length() ? content.length() : index+200);
		
		//System.out.println(content);
		
		Matcher matcher=p.matcher(content);
		if(!matcher.find()) {
			throwProtocolChanged();
		}
		return matcher.group(1);
	}
	
	/**
	 * Simulate going to the Export Contacts page and then clicking the export button 
	 * @author Thomas BERNARD
	 */
	protected InputStream getContactListContent(DefaultHttpClient client, String listUrl, String referer) throws ContactListImporterException, URISyntaxException, InterruptedException, HttpException, IOException {
		InputStream page = doGet(client, listUrl, referer);
		BufferedReader blop = new BufferedReader(new InputStreamReader(page, "CP1252")); // we could have the charset from the entity
		// get values for form :
		// <input type="hidden" name="__VIEWSTATE" id="__VIEWSTATE" value="/wEPDwULLTE5Mzg1OTc0NjQPZBYCAgkPZBYCZg9kFgJmD2QWAmYPDxYCHgRUZXh0BRVFeHBvcnRlciBkZXMgY29udGFjdHNkZGRhOT6nOnCRJvBE38tzct7mj9mv4g==" />
		Pattern pViewState = Pattern.compile(".*name=\"__VIEWSTATE\".*value=\"([^\"]*)\".*");
		String viewState = null;
		// <input type="hidden" name="__EVENTVALIDATION" id="__EVENTVALIDATION" value="/wEWAgKLx4TKCALy4eKoCKyMGoHSihkXecaJI7cozQvjMqGz" />
		Pattern pEventValidation = Pattern.compile(".*name=\"__EVENTVALIDATION\".*value=\"([^\"]*)\".*");
		String eventValidation = null;
		String line = blop.readLine();
		while(line != null && (viewState == null || eventValidation == null)) {
			line = blop.readLine();
			
			//System.out.println(line);
			
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
		page.close();
		if(viewState == null || eventValidation == null) {
			// NOT GOOD !
			throw new ContactListImporterException("Could not fill form for getting CSV from Hotmail");
		}
		String mt = "";
		for(Cookie cookie : client.getCookieStore().getCookies()) {
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
		return doPost(client, listUrl, data, listUrl);
	}

	/**
	 * retrieve the action attribute value of a HTML form
	 * @param content HTML document
	 * @return a URL
	 * @throws ContactListImporterException
	 */
	private String getFormUrl(String content) throws ContactListImporterException {
		int begin = content.indexOf("<form")+5;
		int end = content.indexOf("</form>", begin);
		content = content.substring(begin, end);
		String[] attributes = content.split("\\s+");
		Pattern p = Pattern.compile("action=\"([^\\s\"]+)\"");
		for(int i = 0; i < attributes.length ; i++) {
			Matcher matcher = p.matcher(attributes[i]);
			if(matcher.find()) {
				return matcher.group(1);
			}
		}
		throwProtocolChanged();
		// return statement required but never reachable
		return null;
	}
	
	private void throwProtocolChanged() throws ContactListImporterException {
		throw new ContactListImporterException("Microsoft hotmail changed it's protocol, cannot import contactslist");
	}

	public static boolean isHotmail(String email) {
		/*
		String[] domains={
			"hotmail.com", 
			"hotmail.co.uk",
			"hotmail.fr",
			"live.com",
			"live.nl",
			"live.ca",
			"live.co.uk",
			"live.fr",
			"live.com.au",
			"msn.com"
		};
		return ContactListImporterImpl.isConformingEmail(email, domains);
		*/
		return (email.indexOf("@live.") != -1) || (email.indexOf("@hotmail.") != -1) || (email.indexOf("@msn.com") != -1); 
	}
}
