package com.xdatasystem.contactsimporter;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.UrlEncodedFormEntity;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HTTP;

/**
 * Class that acts as a webbrowser.
 * It holds all the http state, this allows easy
 * decoupling of both the importer and the webclient.
 * <br/><br/>
 * It encapsulates an HttpClient and
 * gives some util functions that delegate actions
 * to the HttpClient.
 * 
 * @author Tjerk Wolterink
 */
public class WebClient {
	private DefaultHttpClient client;
	private String currentURL = null;
	private static Logger log = Logger.getLogger(ContactListImporterImpl.class.getName());
	
	public WebClient() {
		client=new DefaultHttpClient();
		client.setCookieStore(new UpdateableCookieStore());
		client.setRedirectHandler(new MemorizingRedirectHandler());
		client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);
	
		List<Header> headers=new ArrayList<Header>();
		headers.add(new BasicHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; nl; rv:1.8.1.13) Gecko/20080311 Firefox/2.0.0.13"));
		client.getParams().setParameter(ClientPNames.DEFAULT_HEADERS, headers);
	}
	
	public String getHostName() {
		return ((HttpHost)getHttpClient().getDefaultContext().getAttribute(
	   		ExecutionContext.HTTP_TARGET_HOST)
	  ).getHostName();
	}
	
	/**
	 * Gets a default HttpClient that mimics the beviour of Firefox 2.
	 * Redirects are followed automatically.
	 */
	public DefaultHttpClient getHttpClient() {
		return client;
	}
	
	protected void setHeaders(HttpRequest req, String referer) {
		// mimic firefox headers
		//req.addHeader("User-Agent", "Mozilla/5.0 (Windows; U; Windows NT 5.1; nl; rv:1.8.1.13) Gecko/20080311 Firefox/2.0.0.13");
		req.addHeader("Accept", "text/xml,text/javascript,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5");
	  req.addHeader("Accept-Language", "en-us;q=0.7,en;q=0.3");
	  req.addHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
	  if(referer!=null) {
	  	req.addHeader("Referer", referer);
	  }
	}
	
	/**
	 * Update where we are now. through 302/303 redirections we could get lost !
	 * @param client
	 */
	private void updateCurrentUrl() {
		HttpRequest req = (HttpRequest)client.getDefaultContext().getAttribute(ExecutionContext.HTTP_REQUEST);
		HttpHost host = (HttpHost)client.getDefaultContext().getAttribute(ExecutionContext.HTTP_TARGET_HOST);
		currentURL = host.toURI() + req.getRequestLine().getUri();
	}
	
	/**
	 * @return the current url
	 */
	public String getCurrentUrl() {
		return this.currentURL;
	}
	
	/**
	 * Performs a http GET operation
	 * 
	 * @param client the client performing the request
	 * @param url the url to retrieve
	 * @param referer a possibly needed ref url, or null otherwise
	 * @return the InputStream that contains the content
	 */
	public InputStream doGet(String url, String referer)
		throws ContactListImporterException, URISyntaxException, InterruptedException, HttpException, IOException
	{
		client.getConnectionManager().closeIdleConnections(0, TimeUnit.MILLISECONDS);
		HttpGet get=new HttpGet(url);
		setHeaders(get, referer);
    HttpResponse resp=client.execute(get, client.getDefaultContext());
    //if (statusCode!=resp.get) {
    //	throw new ContactListImporterException("Page GET request failed NOK: "+get.getStatusLine());
    //}
    updateCurrentUrl();
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
	public InputStream doPost(String url, NameValuePair[] data, String referer)
		throws ContactListImporterException, HttpException, IOException, InterruptedException, URISyntaxException
	{
		log.info("POST " + url);
		//client.getConnectionManager().closeIdleConnections(0, TimeUnit.MILLISECONDS);
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
   	updateCurrentUrl();
    InputStream content=resp.getEntity().getContent();
    return content;
	}
	
}
