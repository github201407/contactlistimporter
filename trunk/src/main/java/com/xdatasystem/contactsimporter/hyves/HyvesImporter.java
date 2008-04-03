package com.xdatasystem.contactsimporter.hyves;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.UrlEncodedFormEntity;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import com.xdatasystem.contactsimporter.AuthenticationException;
import com.xdatasystem.contactsimporter.ContactImpl;
import com.xdatasystem.contactsimporter.ContactListImporterException;
import com.xdatasystem.contactsimporter.ContactListImporterImpl;
import com.xdatasystem.user.Contact;

/**
 * Contact importer for the hyves social network site: 
 * http://www.hyves.nl
 * <br/><br/>
 * Hyves is holland what facebook and myspace is
 * internationally.
 * <br/><br/>
 * This importer is a bit slow because the contacts
 * can only be retrieved page by page.
 * 
 * @author Tjerk Wolterink
 */
public class HyvesImporter extends ContactListImporterImpl {
	private String extraField;
	
	public HyvesImporter(String username, String password) {
		super(username, password);
	}
	
	@Override
	public String getContactListURL() {
		return "http://www.hyves.nl/index.php?xmlHttp=1&module=pager&action=showPage";
	}

	@Override
	public String getLoginURL() {
		// TODO Auto-generated method stub
		return "http://www.hyves.nl/?module=authentication&action=basicLogin";
	}

	@Override
	protected void login(DefaultHttpClient client) throws Exception {
		// get required cookies
		InputStream is=this.doGet(client, "http://www.hyves.nl", "");
		while(is.read()!=-1) {}
		is.close();
		
		
		NameValuePair[] data = {
			new BasicNameValuePair("login_username", this.getUsername()),
			new BasicNameValuePair("login_password", this.getPassword()),
			new BasicNameValuePair("btnLogin", "Ok"),
			new BasicNameValuePair("login_initialPresence", "offline"),
			new BasicNameValuePair("auth_currentUrl", "http://www.hyves.nl/berichten/contacts/"),
		};
		String content=this.readInputStream(
			this.doPost(client, this.getLoginURL(), data, "http://www.hyves.nl/")
		);
		
		if(content.contains("combination is unknown")) {
			throw new AuthenticationException("Username and password do not match");
			
		}
	}
	
	private String getExtraField(DefaultHttpClient client) throws ContactListImporterException, IOException, URISyntaxException, InterruptedException, HttpException {
		getLogger().info("Retrieve first hyves contacts page");
		
		String content=this.readInputStream(
			this.doGet(client, "http://www.hyves.nl/berichten/contacts/", "http://www.hyves.nl")
		);
		
		String prePattern="extra: '";
		int index=content.indexOf(prePattern);
		if(index==-1) {
			throw new ContactListImporterException("Hyves changed protocols, the extra field could not be found");
		}
		String newContent=content.substring(index+prePattern.length());
		index=newContent.indexOf("'");
		if(index==-1) {
			throw new ContactListImporterException("Hyves changed protocols, the extra field could not be found");
		}
		this.extraField=newContent.substring(0, index);
		return content;
	}

	@Override
	protected List<Contact> parseContacts(InputStream contactsContent) throws Exception {
		// does nothing,
		// hyves contacts can only be retrieved
		// page by page
		return null;
	}
	
	protected List<Contact> getAndParseContacts(DefaultHttpClient client, String host) throws Exception {
		
		getLogger().info("Retrieving contactlist");
		List<Contact> contacts=new ArrayList<Contact>(80);
		boolean doNext;
		int pageNr=1;
		String content=getExtraField(client);
		
		parseAndAdd(content, contacts);
		
		do {
			pageNr++;
			doNext=addContacts(client, contacts, pageNr);
		} while(doNext);
		
		return contacts;
	}
	
	protected boolean addContacts(DefaultHttpClient client, List<Contact> contacts, int pageNr) throws ContactListImporterException, URISyntaxException, InterruptedException, HttpException, IOException {
		String listUrl=getContactListURL();
		
		getLogger().info("Retrieve hyves contacts page "+pageNr);
		
		NameValuePair[] data = {
			new BasicNameValuePair("name", "member_friend"),
			new BasicNameValuePair("pageNr", ""+pageNr),
			new BasicNameValuePair("config", "hyvespager-config.php"),
			new BasicNameValuePair("extra", this.extraField)
		};
		

		HttpPost post=new HttpPost(listUrl);
		
		super.setHeaders(post, "http://www.hyves.nl/berichten/contacts/");
		post.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
		post.addHeader("X-Requested-With", "XMLHttpRequest");
		post.addHeader("X-Prototype-Version", "1.6.0.2");
		
		
		post.setEntity(new UrlEncodedFormEntity(data, HTTP.UTF_8));
		HttpProtocolParams.setUseExpectContinue(client.getParams(), false);
		HttpProtocolParams.setUseExpectContinue(post.getParams(), false);
   	HttpResponse resp=client.execute(post, client.getDefaultContext());
   	
    //if (statusCode!=HttpStatus.SC_OK) {
    //	throw new ContactListImporterException("Page GET request failed NOK: "+post.getStatusLine());
    //}
    return parseAndAdd(
    	readInputStream(resp.getEntity().getContent()),
    	contacts
    );
	}

	private boolean parseAndAdd(String content, List<Contact> contacts) throws IOException {
		getLogger().info("Parsing hyves contacts page");
		
		
		String beginPart="width=\"40%\">";
		int index;
		int lastIndex;
		boolean isFirst=true;
		
		while(true) {
			index=content.indexOf(beginPart);
			if(index==-1) {
				if(isFirst) return false;
				else {
					break;
				}
			}
			
			content=content.substring(index+beginPart.length());
			isFirst=false;
			
			index=content.indexOf("\">");
			lastIndex=content.indexOf("</a>");
			if(index==-1 || lastIndex==-1) continue;
			String name=content.substring(index+2, lastIndex).trim();
			
			index=content.indexOf("<td>");
			if(index==-1) continue;
			content=content.substring(index+4);
			
			lastIndex=content.indexOf("</td>");
			if(lastIndex==-1) continue;
			String email=content.substring(0, lastIndex).trim();
			
			index=email.indexOf(">");
			if(index>0) {
				email=email.substring(index+1);
			}
			index=email.indexOf("<");
			if(index>0) {
				email=email.substring(0, index);
			}
			email=email.toLowerCase();
			
			if(email.length()==0) continue;
			if(name.length()==0) {
				name=email.substring(0, email.indexOf("@"));
			}
			
			if(isEmailAddress(email)) {
				
				Contact contact=new ContactImpl(name, email);
				if(!contacts.contains(contact)) {
					contacts.add(contact);
				}
			}
		}
		
		return true;
		
	}

}
