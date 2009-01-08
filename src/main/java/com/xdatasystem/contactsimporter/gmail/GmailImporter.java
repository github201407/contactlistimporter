package com.xdatasystem.contactsimporter.gmail;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpException;
import org.apache.http.NameValuePair;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import com.xdatasystem.contactsimporter.AuthenticationException;
import com.xdatasystem.contactsimporter.ContactImpl;
import com.xdatasystem.contactsimporter.ContactListImporterException;
import com.xdatasystem.contactsimporter.ContactListImporterImpl;
import com.xdatasystem.contactsimporter.UpdateableCookieStore;
import com.xdatasystem.contactsimporter.WebClient;
import com.xdatasystem.user.Contact;

/**
 * Contacts importer for the gmail service.
 * 
 * @author Tjerk Wolterink
 */
public class GmailImporter extends ContactListImporterImpl {

	public GmailImporter(String username, String password) {
		super(username, password);
	}
	
	@Override
	public String getContactListURL() {
		return "https://mail.google.com/mail/contacts/data/contacts?thumb=true&show=ALL&enums=true&psort=Name&max=10000&out=js&rf=&jsx=true";
	}

	@Override
	public String getLoginURL() {
		return "https://www.google.com/accounts/ServiceLoginAuth";
	}

	@Override
	protected void login(WebClient client) throws ContactListImporterException, IOException, URISyntaxException,
			InterruptedException, HttpException {
		
		NameValuePair[] data = {
			new BasicNameValuePair("ltmpl", "yj_blanco"),
			new BasicNameValuePair("continue", "https://mail.google.com/mail/"),
			new BasicNameValuePair("ltmplcache", "2"),
			new BasicNameValuePair("service", "mail"),
			new BasicNameValuePair("rm", "false"),
			//new BasicNameValuePair("ltmpl", "yj_blanco"),
			new BasicNameValuePair("hl", "en"),
			new BasicNameValuePair("Email", this.getUsername()),
			new BasicNameValuePair("Passwd", this.getPassword()),
			new BasicNameValuePair("rmShown", "1"),
			new BasicNameValuePair("null", "Sign in")
		};
		
		// security cookie
		long time=System.currentTimeMillis();
		BasicClientCookie cookie=new BasicClientCookie(
			"GMAIL_LOGIN",
			"T"+time+"/"+(time-16)+"/"+time
		);
		client.getHttpClient().getCookieStore().addCookie(cookie);
		
		String content=this.readInputStream(
			client.doPost(this.getLoginURL(), data, "")
		);
		if(content.contains("Username and password do not match")) {
			throw new AuthenticationException("Username and password do not match");
			
		} else if(content.contains("Required field must not be blank")) {
			throw new ContactListImporterException("Required field must not be blank");
			
		} else if(content.contains("errormsg_0_logincaptcha")) {
			throw new ContactListImporterException("Captcha error");
			
		} else if(content.contains("Invalid request")) {
			throw new ContactListImporterException("Invalid Request, reason unkown");
			
		}
		
		UpdateableCookieStore cookies=(UpdateableCookieStore)client.getHttpClient().getCookieStore();
		cookies.removeCookie("LSID");
		cookies.removeCookie("GB");

	}

	@Override
	protected List<Contact> parseContacts(InputStream contactsContent) throws IOException, JSONException, ContactListImporterException {
		String json=this.readInputStream(contactsContent);
		
		String startTag="&&&START&&&";
		String endTag="&&&END&&&";
		json=json.substring(
			json.indexOf(startTag)+startTag.length(),
			json.indexOf(endTag)
		);
		
		JSONTokener jsonTokener=new JSONTokener(json);
		Object o=jsonTokener.nextValue();
		if(o==null || !(o instanceof JSONObject)) {
			throw new ContactListImporterException("Gmail contactlist format changed, cannot parse contacts");
		}
		JSONObject jsonObj=(JSONObject)o;
		jsonObj=jsonObj.getJSONObject("Body");
		JSONArray jsonContacts=jsonObj.getJSONArray("Contacts");
		
		List<Contact> contacts=new ArrayList<Contact>(jsonContacts.length());
		for(int i=0;i<jsonContacts.length();i++) {
			jsonObj=jsonContacts.getJSONObject(i);
			
			String name=null;
			if(jsonObj.has("Name")) {
				name=jsonObj.getString("Name");
			}
			if(!jsonObj.has("Emails")) continue;
			JSONArray emails=jsonObj.getJSONArray("Emails");
			
			for(int j=0;j<emails.length();j++) {
				jsonObj=emails.getJSONObject(j);
				if(!jsonObj.has("Address")) continue;
				String email=jsonObj.getString("Address");
				
				int atIndex=email.indexOf("@");
				// only add contacts that have an email adress
				if(atIndex==-1) continue;
				
				if(name==null || name.length()==0) {
					name=email.substring(0, email.indexOf("@"));
				}
				email=email.toLowerCase();
				
				if(isEmailAddress(email)) {
					contacts.add(
						new ContactImpl(name, email)
					);
				}
			}
		}
		
		return contacts;
	}

	public static boolean isGmail(String email) {
		String[] domains={
			"gmail.com",
			"googlemail.com"
		};
		return ContactListImporterImpl.isConformingEmail(email, domains);
	}

}
