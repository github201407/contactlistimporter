package com.xdatasystem.contactsimporter;

import java.util.Collection;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieOrigin;
import org.apache.http.cookie.CookieSpecRegistry;
import org.apache.http.cookie.MalformedCookieException;
import org.apache.http.impl.cookie.BrowserCompatSpec;

public class LenientCookieSpec extends BrowserCompatSpec {//extends BrowserCompatSpec {
	public static final String ID="LenientCookieSpec";

	LenientCookieSpec() {}
	LenientCookieSpec(String[] params) { super(params); }
	
	public static void register() {
		//CookieSpecRegistry.r
	}
	
	public List<Cookie> parse(final Header header, final CookieOrigin origin) throws MalformedCookieException {
		List<Cookie> cookies=super.parse(header, origin);
		System.out.println("Cookies extracted from "+header.getValue());
		for(Cookie c : cookies) {
			System.out.println(" - "+c.getName()+" = "+c.getValue());
		}
		return cookies;
	}
}
