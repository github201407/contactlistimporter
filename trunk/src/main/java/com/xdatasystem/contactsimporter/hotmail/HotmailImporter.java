package com.xdatasystem.contactsimporter.hotmail;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.cookie.*;
import org.apache.commons.httpclient.methods.*;
import org.apache.commons.httpclient.params.*;
import com.xdatasystem.contactsimporter.*;


public class HotmailImporter extends ContactListImporterImpl {

	public HotmailImporter(String username, String password) {
		super(username, password);
	}

	@Override
	public List<Contact> getContactList() throws IOException, ContactListImporterException {
		
		HttpClient client=new HttpClient();
    
    GetMethod login=new GetMethod(getLoginURL().toString());
    login.getParams().setParameter(HttpMethodParams.RETRY_HANDLER, 
    	new DefaultHttpMethodRetryHandler(3, false)
    );
    login.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
    client.executeMethod(login);
    
    int statusCode=client.executeMethod(login);
    
    if (statusCode!=HttpStatus.SC_OK) {
    	throw new ContactListImporterException("Login page http respons NOK: "+login.getStatusLine());
    }
    String content=new String(login.getResponseBody());
		// TODO:
    //String ppsx=getInputValue("PPSX");
		//String ppft=getInputValue("PPFT");
		
		
		
		return null;
		
	}
	
	@Override
	public URL getLoginURL() {
		try {
			return new URL("http://login.live.com/login.srf?id=2?");
		} catch(MalformedURLException e) {
			// should never occur
			throw new Error(e);
		}
	}

	@Override
	public URL getContactListURL() {
		try {
			return new URL("http://login.live.com/login.srf?id=2?");
		} catch(MalformedURLException e) {
			// should never occur
			throw new Error(e);
		}
	}

}
