package com.xdatasystem.contactsimporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.client.HttpClient;
import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.RedirectLocations;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;

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

	public ContactListImporterImpl(String username, String password) {
		this.username=username;
		this.password=password;
	}
	
	protected String getUsername() {
		return username;
	}
	
	protected String getPassword() {
		return password;
	}
	
	public abstract String getLoginURL();
	public abstract String getContactListURL();
	
	public List<Contact> getContactList() throws ContactListImporterException {

		//System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
		//System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "debug");
		
		try {
			
			DefaultHttpClient client=this.getHttpClient();
			login(client);
			
	   	String host=((HttpHost)client.getDefaultContext().getAttribute(
	   		ExecutionContext.HTTP_TARGET_HOST)
	   	).getHostName();
	   	
			String listUrl=String.format(getContactListURL(), host);
	    return parseContacts(this.doGet(client, listUrl, null));
	    
		} catch(IOException e) {
			throw new ContactListImporterException("IOException occured", e);
			
		} catch(URISyntaxException e) {
			throw new ContactListImporterException("URISyntaxException occured", e);
			
		} catch(InterruptedException e) {
			throw new ContactListImporterException("InterruptedException occured", e);
			
		} catch(HttpException e) {
			throw new ContactListImporterException("HttpException occured", e);
		}
	}
	
	/**
	 * Performs the login. The http client is logged in after this method call.
	 * 
	 * @return the current host location url.
	 */
	protected abstract void login(DefaultHttpClient client)
	throws ContactListImporterException, IOException, URISyntaxException, InterruptedException, HttpException;
	
	/**
	 * Parses the contactContent string that was retrieved and
	 * returns the contacts as a list
	 * 
	 * @para, contactsContent the content of the contacts file retrieved from the server
	 * @return a list of contacts parsed from the contactsContent
	 */
	protected abstract List<Contact> parseContacts(InputStream contactsContent) throws IOException;

	
	/**
	 * Gets a default HttpClient that mimics the beviour of Firefox 2.
	 * Redirects are followed automatically.
	 */
	protected DefaultHttpClient getHttpClient() {
		DefaultHttpClient client=new DefaultHttpClient();
		client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
		
		List<Header> headers=new ArrayList<Header>();
		headers.add(new BasicHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; nl; rv:1.8.1.13) Gecko/20080311 Firefox/2.0.0.13"));
		client.getParams().setParameter(ClientPNames.DEFAULT_HEADERS, headers);
		
		return client;
	}
	
	private void setHeaders(HttpRequest req, String referer) {
		// mimic firefox headers
		req.addHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; nl; rv:1.8.1.13) Gecko/20080311 Firefox/2.0.0.13");
		req.addHeader("Accept", "text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
	  req.addHeader("Accept-Language", "en-us;q=0.7,en;q=0.3");
	  req.addHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
	  if(referer!=null) {
	  	req.addHeader("Referer", referer);
	  }
	}
	
	/**
	 * Performs a http GET operation
	 * 
	 * @param client the client performing the request
	 * @param url the url to retrieve
	 * @param referer a possibly needed ref url, or null otherwise
	 * @return the InputStream that contains the content
	 */
	protected InputStream doGet(HttpClient client, String url, String referer)
		throws ContactListImporterException, URISyntaxException, InterruptedException, HttpException, IOException
	{
		
		HttpGet get=new HttpGet(url);
		setHeaders(get, referer);
    System.out.println("Client requesting: "+url);
    HttpResponse resp=client.execute(get, client.getDefaultContext());
    System.out.println("Get Status: "+resp.getStatusLine());
    //if (statusCode!=resp.get) {
    //	throw new ContactListImporterException("Page GET request failed NOK: "+get.getStatusLine());
    //}
    return resp.getEntity().getContent();
	}

	/**
	 * Performs a http POST operation
	 * 
	 * @param client the client performing the request
	 * @param url the url to retrieve
	 * @param referer a possibly needed ref url, or null otherwise
	 * @return the InputStream that contains the content
	 */
	protected InputStream doPost(HttpClient client, String url, NameValuePair[] data, String referer)
		throws ContactListImporterException, HttpException, IOException, InterruptedException, URISyntaxException
	{
		
		HttpPost post=new HttpPost(url);
		setHeaders(post, referer);
		post.addHeader("Content-Type", "application/x-www-form-urlencoded");
		post.setEntity(new UrlEncodedFormEntity(data, HTTP.UTF_8));
		HttpProtocolParams.setUseExpectContinue(client.getParams(), false);
		HttpProtocolParams.setUseExpectContinue(post.getParams(), false);
   	HttpResponse resp=client.execute(post, client.getDefaultContext());
   	System.out.println(resp.getParams().toString());
   
   	System.out.println("PostStatus: "+resp.getStatusLine());
   	

   	
    //if (statusCode!=HttpStatus.SC_OK) {
    //	throw new ContactListImporterException("Page GET request failed NOK: "+post.getStatusLine());
    //}
    return resp.getEntity().getContent();
	}
	
	protected String readInputStream(InputStream is) throws IOException {
		BufferedReader in=new BufferedReader(new InputStreamReader(is));
		StringBuffer buffer=new StringBuffer();
		String line;
		while ((line = in.readLine()) != null) {
			buffer.append(line);
		}
		return buffer.toString();
	}

}
