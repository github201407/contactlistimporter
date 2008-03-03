package com.xdatasystem.contactsimporter;

public class Contact {
	private String name;
	private String email;
	
	public Contact(String name, String email) {
		this.name=name;
		this.email=email;
	}
	
	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}
	
	public String toString() {
		return "name: "+name+", email: "+email;
	}

}
