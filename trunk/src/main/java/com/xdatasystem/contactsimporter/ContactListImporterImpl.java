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
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;
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
import com.xdatasystem.contactsimporter.hotmail.HotmailImporter;
import com.xdatasystem.user.Contact;

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
	private static Logger log=Logger.getLogger(HotmailImporter.class.getPackage().getName());
	private Pattern emailPattern;
	private DefaultHttpClient client;

	public ContactListImporterImpl(String username, String password) {
		this.username=username;
		this.password=password;
		emailPattern=Pattern.compile(
			"^[0-9a-z]([-_.~]?[0-9a-z])*@[0-9a-z]([-.]?[0-9a-z])*\\.[a-z]{2,4}$"
		);
	}
	
	protected Logger getLogger() {
		return log;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public abstract String getLoginURL();
	public abstract String getContactListURL();
	
	public boolean isEmailAddress(String email) {
		return emailPattern.matcher(email).matches();
	}
	
	public List<Contact> getContactList() throws ContactListImporterException {

		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
		//System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "debug");
		
		try {
			
			DefaultHttpClient client=this.getHttpClient();
			log.info("Performing login");
			login(client);
			log.info("Login succeeded");
			
	   	String host=((HttpHost)client.getDefaultContext().getAttribute(
	   		ExecutionContext.HTTP_TARGET_HOST)
	   	).getHostName();
	   	
			return this.getAndParseContacts(client, host);
	    
		} catch(Exception e) {
			if(e instanceof ContactListImporterException) {
				throw (ContactListImporterException)e;
			}
			throw new ContactListImporterException("Exception occured: "+e.getMessage(), e);
		}
	}
	
	protected List<Contact> getAndParseContacts(DefaultHttpClient client, String host) throws Exception {
		
		String listUrl=String.format(getContactListURL(), host);
		log.info("Retrieving contactlist");
		InputStream input=this.getContactListContent(client, listUrl, null);
		log.info("Parsing contactlist");
    return parseContacts(input);
	}

	/**
	 * Gets the contact list using HTTP Get,
	 * override to implement your own implementation
	 * 
	 * @return the content of the contact list as an inputstream
	 */
	protected InputStream getContactListContent(DefaultHttpClient client, String listUrl, String referer) throws ContactListImporterException, URISyntaxException, InterruptedException, HttpException, IOException {
		return this.doGet(client, listUrl, referer);
	}

	/**
	 * Performs the login. The http client is logged in after this method call.
	 * 
	 * @return the current host location url.
	 */
	protected abstract void login(DefaultHttpClient client)
	throws Exception;
	
	/**
	 * Parses the contactContent string that was retrieved and
	 * returns the contacts as a list
	 * 
	 * @para, contactsContent the content of the contacts file retrieved from the server
	 * @return a list of contacts parsed from the contactsContent
	 */
	protected abstract List<Contact> parseContacts(InputStream contactsContent) throws Exception;

	
	/**
	 * Gets a default HttpClient that mimics the beviour of Firefox 2.
	 * Redirects are followed automatically.
	 */
	protected DefaultHttpClient getHttpClient() {
		if(this.client==null) {
			client=new DefaultHttpClient();
			client.setCookieStore(new UpdateableCookieStore());
			client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
			
			List<Header> headers=new ArrayList<Header>();
			headers.add(new BasicHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; nl; rv:1.8.1.13) Gecko/20080311 Firefox/2.0.0.13"));
			client.getParams().setParameter(ClientPNames.DEFAULT_HEADERS, headers);
			
			return client;
		} else {
			return client;
		}
	}
	
	protected void setHeaders(HttpRequest req, String referer) {
		// mimic firefox headers
		req.addHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; nl; rv:1.8.1.13) Gecko/20080311 Firefox/2.0.0.13");
		req.addHeader("Accept", "text/xml,text/javascript,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
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
		client.getConnectionManager().closeIdleConnections(0, TimeUnit.MILLISECONDS);
		HttpGet get=new HttpGet(url);
		setHeaders(get, referer);
    HttpResponse resp=client.execute(get, client.getDefaultContext());
    //if (statusCode!=resp.get) {
    //	throw new ContactListImporterException("Page GET request failed NOK: "+get.getStatusLine());
    //}
    InputStream content=resp.getEntity().getContent();
    return content;
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
		client.getConnectionManager().closeIdleConnections(0, TimeUnit.MILLISECONDS);
		HttpPost post=new HttpPost(url);
		setHeaders(post, referer);
		post.addHeader("Content-Type", "application/x-www-form-urlencoded");
		
		post.setEntity(new UrlEncodedFormEntity(data, HTTP.UTF_8));
		HttpProtocolParams.setUseExpectContinue(client.getParams(), false);
		HttpProtocolParams.setUseExpectContinue(post.getParams(), false);
   	HttpResponse resp=client.execute(post, client.getDefaultContext());
   	
    //if (statusCode!=HttpStatus.SC_OK) {
    //	throw new ContactListImporterException("Page GET request failed NOK: "+post.getStatusLine());
    //}
    InputStream content=resp.getEntity().getContent();
    return content;
	}
	
	/**
	 * Reads an inputstream and converts it to a string.
	 * Note that this is rather memory intensive, if you
	 * do not need random access in the inputstream you
	 * should iterate sequentially over the lines using
	 * readLine()
	 * 
	 * @param is the inputstream to convert
	 * @return the content of the input stream
	 * @throws IOException if reading the inputstream fails
	 */
	protected String readInputStream(InputStream is) throws IOException {
		BufferedReader in=new BufferedReader(new InputStreamReader(is));
		StringBuffer buffer=new StringBuffer();
		String line;
		while ((line = in.readLine()) != null) {
			buffer.append(line);
		}
		is.close();
		return buffer.toString();
	}
	
	/**
	 * Wether the email ends with one of the domains
	 * in the domains list.
	 * 
	 * @param email the email tot test
	 * @param domains a list of domains. 
	 * @return true if the email ends with one of the domains in the domains list
	 */
	public static boolean isConformingEmail(String email, String[] domains) {
		if(email==null) return false;
		for(String d : domains) {
			if(email.indexOf(d)==email.length()-d.length()) {
				return true;
			}
		}
		return false;
	}

}
