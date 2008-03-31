package com.xdatasystem.contactsimporter;

/**
 * Simple representation for a contact.
 * A contact has a name and an email;
 * 
 * @author Tjerk Wolterink
 */
public class Contact {
	private String name;
	private String email;
	
	public Contact(String name, String email) {
		this.name=name;
		this.email=email;
	}
	
	/**
	 * Returns the name of the contact.
	 * Note that some contactImporters cannot
	 * retrieve the name: In that case the name equals
	 * the email address.
	 * 
	 * @return the name of the contact
	 */
	public String getName() {
		return name;
	}

	/**
	 * Returns the email address of the contact
	 * 
	 * @return the email address
	 */
	public String getEmail() {
		return email;
	}
	
	public String toString() {
		return "name: "+name+", email: "+email;
	}

}
