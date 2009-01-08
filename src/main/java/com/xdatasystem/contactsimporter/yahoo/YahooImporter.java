package com.xdatasystem.contactsimporter.yahoo;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpException;
import org.apache.http.NameValuePair;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import au.com.bytecode.opencsv.CSVReader;
import com.xdatasystem.contactsimporter.AuthenticationException;
import com.xdatasystem.contactsimporter.ContactImpl;
import com.xdatasystem.contactsimporter.ContactListImporterException;
import com.xdatasystem.contactsimporter.ContactListImporterImpl;
import com.xdatasystem.contactsimporter.WebClient;
import com.xdatasystem.user.Contact;

/**
 * Contacts importer for the yahoo service.
 * Only imports contacts that have an email adress.
 *
 * @author Cristian Ventura
 * @author Tjerk Wolterink
 */
public class YahooImporter extends ContactListImporterImpl {

	/**
	 * Constructs a YahooImporter.
	 * We recommand usage of the ContactListImporterFactory factory object to
	 * instantiate this importer..
	 * This decouples instantiation from implementation, and follows
	 * the Factory Software Pattern.
	 */
	public YahooImporter(String username, String password) {
		super(username, password);
	}

	private String crumb = "";

	@Override
	public String getContactListURL() {
		return "http://address.yahoo.com/index.php?VPC=import_export&A=B&submit[action_export_yahoo]=Export+Now" + "&.crumb=" + crumb;
	}

	@Override
	public String getLoginURL() {
		return "https://login.yahoo.com/config/login";
	}

	private static final String ADDRESS_BOOK_URL = "http://address.mail.yahoo.com/?1&VPC=import_export";

	@Override
	protected void login(WebClient client) throws ContactListImporterException, IOException, URISyntaxException,
			InterruptedException, HttpException {

		NameValuePair[] data = {
			new BasicNameValuePair(".tries", "2"),
			new BasicNameValuePair(".src", "ym"),
			new BasicNameValuePair(".md5", ""),
			new BasicNameValuePair(".hash", ""),
			new BasicNameValuePair(".js=", ""),
			new BasicNameValuePair(".last", ""),
			new BasicNameValuePair("promo", ""),
			new BasicNameValuePair(".intl", "us"),
			new BasicNameValuePair(".bypass", ""),
			new BasicNameValuePair(".partner", ""),
			new BasicNameValuePair(".u", "4eo6isd23l8r3"),
			new BasicNameValuePair(".v", "0"),
			new BasicNameValuePair(".challenge", "gsMsEcoZP7km3N3NeI4mXkGB7zMV"),
			new BasicNameValuePair(".yplus", ""),
			new BasicNameValuePair(".emailCode", ""),
			new BasicNameValuePair("pkg", ""),
			new BasicNameValuePair("stepid", ""),
			new BasicNameValuePair(".ev", ""),
			new BasicNameValuePair("hasMsgr", "1"),
			new BasicNameValuePair(".chkP", "Y"),
			new BasicNameValuePair(".done", "http://mail.yahoo.com/"),

			new BasicNameValuePair("login", this.getUsername()),
			new BasicNameValuePair("passwd", this.getPassword())
		};

		// security cookie
		long time=System.currentTimeMillis();
		BasicClientCookie cookie=new BasicClientCookie(
			"YAHOO_LOGIN",
			"T"+time+"/"+(time-16)+"/"+time
		);
		client.getHttpClient().getCookieStore().addCookie(cookie);

		String content=this.readInputStream(
			client.doPost(this.getLoginURL(), data, "")
		);
		if(content.contains("Invalid ID or password")) {
			throw new AuthenticationException("Username and password do not match");

		} else if(content.contains("Sign in")) {
			throw new ContactListImporterException("Required field must not be blank");

		} else if(content.contains("errormsg_0_logincaptcha")) {
			throw new ContactListImporterException("Captcha error");

		} else if(content.contains("Invalid request")) {
			throw new ContactListImporterException("Invalid Request, reason unkown");

		}

		// first, get the addressbook site with the new crumb parameter
		data = new NameValuePair[0];
		content=this.readInputStream(
				client.doPost(ADDRESS_BOOK_URL, data, "")
			);

		String subSearch = "id=\"crumb2\" value=\"";
		int pos = content.indexOf(subSearch);
	    crumb = content.substring(content.indexOf("value",pos)+7,content.indexOf(">", pos)-1);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected List<Contact> parseContacts(InputStream contactsContent) throws IOException, JSONException, ContactListImporterException {

		CSVReader csvReader = new CSVReader(new InputStreamReader(contactsContent, "UTF-8"), CSVReader.DEFAULT_SEPARATOR, CSVReader.DEFAULT_QUOTE_CHARACTER, 1);
		List<String[]> myEntries = csvReader.readAll();

		List<Contact> contacts=new ArrayList<Contact>(myEntries.size());

		// TODO validate not empty or nulls with StringUtils (apache)
		String name;
		String email;
		for (String[] entry : myEntries) {
			
			name=entry[0];	// First name
			if(entry[1]!=null && entry[1].length()>0) {
				// 2nd name
				if(name.length() > 0)
					name += " " + entry[1];
				else
					name = entry[1];
			}
			if(entry[2]!=null && entry[2].length()>0) {
				// Last name
				if(name.length() > 0)
					name += " " + entry[2];
				else
					name = entry[2];
			}
			if(entry[3]!=null && entry[3].length()>0) {
				// nickname
				if(name.length() > 0)
					name += " (" + entry[3] +")";
				else
					name = entry[3];
			}
			
			if (!"".equals(entry[4])) {
				// email
			    email = entry[4];
			} else {
				// No Email, only yahoo messenger id. 
				// We cannot guess the domain (it's not always @yahoo.com) !
				continue;
				/*
			    if (!"".equals(entry[7])) {
			        email = entry[7] + "@yahoo.com";
			    } else {
			        email = "";
			        // we want the email adress , so skip this one
			        continue;
			    }
			    */
			}
			String im = "";
			if(!"".equals(entry[7])) {
				im = "ymsgr:" + entry[7];
			}
			if(!"".equals(entry[48])) {
				if(im.length() > 0)
					im = im + ",";
				im += "skype:" + entry[48];				
			}
			if(!"".equals(entry[50])) {
				if(im.length() > 0)
					im = im + ",";
				im += "icq:" + entry[50];				
			}
			if(!"".equals(entry[51])) {
				if(im.length() > 0)
					im = im + ",";
				im += "xmpp:" + entry[51];				
			}
			if(!"".equals(entry[52])) {
				if(im.length() > 0)
					im = im + ",";
				im += "msn:" + entry[52];				
			}
			if(!"".equals(entry[53])) {
				if(im.length() > 0)
					im = im + ",";
				im += "aim:" + entry[53];				
			}
			contacts.add(new ContactImpl(name, email, im));
		}

		return contacts;
	}

	public static boolean isYahoo(String email) {
		/*
		String[] domains={
			"yahoo.com",
			"yahoo.com.ar",
			"yahoo.ru",
			"yahoo.fr",
			"yahoo.co.uk"
		};
		return ContactListImporterImpl.isConformingEmail(email, domains);
		*/
		return (email.indexOf("@yahoo.") != -1);
	}

}
