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
import java.util.logging.Logger;
import java.util.regex.*;

/**
 * Implementation of ContactListImporter that retrieves contacts
 * from microsoft's hotmail service.
 * 
 * @author Tjerk Wolterink
 */
public class HotmailImporter extends ContactListImporterImpl {
	private final static String PWDPAD="IfYouAreReadingThisYouHaveTooMuchFreeTime";
	
	public HotmailImporter(String username, String password) {
		super(username, password);
	}

	@Override
	public String getLoginURL() {
		return "http://login.live.com/login.srf?id=2";
	}

	@Override
	public String getContactListURL() {
		return "http://%s/mail/GetContacts.aspx";
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
    	getLogger().info("Login failed");
	    throw new AuthenticationException("Username and password do not match");
    }	
    
    String redirectLocation=getJSRedirectLocation(content);
    this.doGet(client, redirectLocation, formUrl);
	}

	private String getJSRedirectLocation(String content) throws ContactListImporterException {
		// TODO Auto-generated method stub
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
		BufferedReader in=new BufferedReader(new InputStreamReader(contactsContent));
		String separator=";";
		String line;
		int i=0;
		while ((line = in.readLine()) != null) {
			System.out.println(line);
			if(i>0) {
				if(i==1 && !line.contains(separator)) {
					separator=",";
				}
				String[] values=line.split(separator);
				if(values.length<47) continue;
				String email=parseValue(values[46]);
				if(email.length()==0) continue;
				
				String name=parseValue(values[1]);
				if(values[2].length()>0)
					name+=" "+parseValue(values[2]);
				if(values[3].length()>0)
					name+=" "+parseValue(values[3]);
				if(name.length()==2) name=email.substring(0, email.indexOf("@"));
				
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

	private String getInputValue(String name, String content) throws ContactListImporterException {
		Pattern p=Pattern.compile("^.+value=\"([^\\s\"]+)\"");
		int index=content.indexOf(name)+name.length()+2;
		content=content.substring(index, index+200 > content.length() ? content.length() : index+200);
		
		Matcher matcher=p.matcher(content);
		if(!matcher.find()) {
			throwProtocolChanged();
		}
		return matcher.group(1);
	}
	
	private String getFormUrl(String content) throws ContactListImporterException {
		content=content.substring(content.indexOf("<form")+5);
		String actionAttribute=content.split("\\s+")[5];
		Pattern p=Pattern.compile("action=\"([^\\s\"]+)\"");
		Matcher matcher=p.matcher(actionAttribute);
		if(!matcher.find()) {
			throwProtocolChanged();
		}
		return matcher.group(1);
	}
	
	private void throwProtocolChanged() throws ContactListImporterException {
		throw new ContactListImporterException("Microsoft hotmail changed it's protocol, cannot import contactslist");
	}

	public static boolean isHotmail(String email) {
		String[] domains={
			"hotmail.com",
			"live.com",
			"live.nl",
			"msn.com"
		};
		return ContactListImporterImpl.isConformingEmail(email, domains);
	}
}
