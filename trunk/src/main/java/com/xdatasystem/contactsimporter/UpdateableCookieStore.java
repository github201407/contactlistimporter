package com.xdatasystem.contactsimporter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.cookie.CookieIdentityComparator;

public class UpdateableCookieStore implements CookieStore {

	private final ArrayList<Cookie> cookies;
	private final Comparator<Cookie> cookieComparator;

	/**
	 * Default constructor.
	 */
	public UpdateableCookieStore() {
		super();
		this.cookies=new ArrayList<Cookie>();
		this.cookieComparator=new CookieIdentityComparator();
	}
	
	private int getIndexOfCookie(String cookieName) {
		for(int i=0;i<cookies.size();i++) {
			Cookie c=cookies.get(i);
			if(c.getName().equals(cookieName)) {
				return i;
			}
		}
		return -1;
	}
	
	public Cookie getCookie(String name) {
		int index=getIndexOfCookie(name);
		if(index==-1) return null;
		return cookies.get(index);
	}
	
	public void removeCookie(String name) {
		int index=getIndexOfCookie(name);
		if(index==-1) return;
		cookies.remove(index);
	}

	/**
	 * Adds an {@link Cookie HTTP cookie}, replacing any existing equivalent cookies.
	 * If the given cookie has already expired it will not be added, but existing 
	 * values will still be removed.
	 * 
	 * @param cookie the {@link Cookie cookie} to be added
	 * 
	 * @see #addCookies(Cookie[])
	 * 
	 */
	public synchronized void addCookie(Cookie cookie) {
		if(cookie!=null) {
			// first remove any old cookie that is equivalent
			for(Iterator<Cookie> it=cookies.iterator(); it.hasNext();) {
				if(cookieComparator.compare(cookie, it.next())==0) {
					it.remove();
					break;
				}
			}
			if(!cookie.isExpired(new Date())) {
				cookies.add(cookie);
			}
		}
	}

	/**
	 * Adds an array of {@link Cookie HTTP cookies}. Cookies are added individually and 
	 * in the given array order. If any of the given cookies has already expired it will 
	 * not be added, but existing values will still be removed.
	 * 
	 * @param cookies the {@link Cookie cookies} to be added
	 * 
	 * @see #addCookie(Cookie)
	 * 
	 */
	public synchronized void addCookies(Cookie[] cookies) {
		if(cookies!=null) {
			for(int i=0; i<cookies.length; i++) {
				this.addCookie(cookies[i]);
			}
		}
	}

	/**
	 * Returns an immutable array of {@link Cookie cookies} that this HTTP
	 * state currently contains.
	 * 
	 * @return an array of {@link Cookie cookies}.
	 */
	public synchronized List<Cookie> getCookies() {
		return Collections.unmodifiableList(this.cookies);
	}

	/**
	 * Removes all of {@link Cookie cookies} in this HTTP state
	 * that have expired by the specified {@link java.util.Date date}. 
	 * 
	 * @return true if any cookies were purged.
	 * 
	 * @see Cookie#isExpired(Date)
	 */
	public synchronized boolean clearExpired(final Date date) {
		if(date==null) {
			return false;
		}
		boolean removed=false;
		for(Iterator<Cookie> it=cookies.iterator(); it.hasNext();) {
			if(it.next().isExpired(date)) {
				it.remove();
				removed=true;
			}
		}
		return removed;
	}

	@Override
	public String toString() {
		return cookies.toString();
	}

	/**
	 * Clears all cookies.
	 */
	public synchronized void clear() {
		cookies.clear();
	}

}
