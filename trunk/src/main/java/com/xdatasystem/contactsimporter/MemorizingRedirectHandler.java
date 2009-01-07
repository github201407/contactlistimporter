package com.xdatasystem.contactsimporter;

import java.net.URI;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.impl.client.DefaultRedirectHandler;
import org.apache.http.protocol.HttpContext;

public class MemorizingRedirectHandler extends DefaultRedirectHandler {
	private URI location;
	
	public URI getLocationURI(HttpResponse response, HttpContext context) throws ProtocolException {
		location=super.getLocationURI(response, context);
		return location;
	}
	
	public URI getLastLocation() {
		return location;
	}
}
