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
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import com.xdatasystem.contactsimporter.AuthenticationException;
import com.xdatasystem.contactsimporter.ContactImpl;
import com.xdatasystem.contactsimporter.ContactListImporterException;
import com.xdatasystem.contactsimporter.ContactListImporterImpl;
import com.xdatasystem.contactsimporter.WebClient;
import com.xdatasystem.user.Contact;

/**
 * Generic abstract hotmail importer.
 * Only defines the methods that are shared between the old and the new
 * version of hotmail
 * 
 * @author Tjerk Wolterink
 */
public abstract class AbstractHotmailImporter extends ContactListImporterImpl {
	/**
	 * A message that is used for security when loggin into hotmail.
	 * However it is not really secure :-)
	 */
	protected final static String PWDPAD="IfYouAreReadingThisYouHaveTooMuchFreeTime";
	
	public AbstractHotmailImporter(String username, String password) {
		super(username, password);
	}
	
	@Override
	public String getLoginURL() {
		return "http://login.live.com/login.srf?id=2";
	}

	@Override
	protected void login(WebClient client) throws IOException, ContactListImporterException, URISyntaxException, InterruptedException, HttpException {
		loginAndRedirect(client).close();
	}
	
	/**
	 * Logs in and returns the inputstream to the
	 * page that is redirected.
	 * Be sure to close the stream!
	 */
	protected InputStream loginAndRedirect(WebClient client) throws IOException, ContactListImporterException, URISyntaxException, InterruptedException, HttpException {
		String loginPageUrl=getLoginURL().toString();
		getLogger().info("Requesting login page");
		String content=this.readInputStream(
			client.doGet(loginPageUrl, null)
		);
		
    String ppsx=getInputValue("PPSX", content);
		String ppft=getInputValue("PPFT", content);
		String formUrl=getFormUrl(content);
		//formUrl="https://login.live.com/ppsecure/post.srf";
		
		// security cookie
		BasicClientCookie cookie=new BasicClientCookie("CkTst", "G"+System.currentTimeMillis());
		cookie.setDomain("login.live.com");
		cookie.setPath("/ppsecure/");
		client.getHttpClient().getCookieStore().addCookie(cookie);
		
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
			client.doPost(formUrl, data, loginPageUrl)
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
    return client.doGet(redirectLocation, formUrl);
	}
	
	/**
	 * Read the redirection location from Javascript code.
	 * @param content HTML page
	 * @return the url where the browser would be redirected
	 * @throws ContactListImporterException
	 */
	protected String getJSRedirectLocation(String content) throws ContactListImporterException {
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
	
	
	/**
	 * Chops of the quotes of a string (if any)
	 */
	private String parseValue(String value) {
		// chop off quotes
		if(value.length()>0 && value.charAt(0)=='"') {
			value=value.substring(1, value.length()-1);
		}
		return value;
	}
	
	/**
	 * retrieve the action attribute value of a HTML form
	 * @param content HTML document
	 * @return a URL
	 * @throws ContactListImporterException
	 */
	protected String getFormUrl(String content) throws ContactListImporterException {
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
	
	/**
	 * Get a value from a HTML form
	 * @param name the name of the input element
	 * @param content the HTML document
	 * @return the value of the input element
	 * @throws ContactListImporterException
	 */
	protected String getInputValue(String name, String content) throws ContactListImporterException {
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
	 * Checks wether the variable 'email' is an hotmail adress.
	 * 
	 * @param email the adress to check
	 * @return true if it is (probably) an hotmail adress
	 */
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
