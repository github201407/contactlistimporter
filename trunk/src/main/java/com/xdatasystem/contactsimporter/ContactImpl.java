package com.xdatasystem.contactsimporter;

import com.xdatasystem.user.Contact;

/**
 * Simple representation for a contact.
 * A contact has a name and an email;
 * 
 * @author Tjerk Wolterink
 */
public class ContactImpl implements Contact {
	private String name;
	private String email;
	
	public ContactImpl(String name, String email) {
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
	public String getEmailAddress() {
		return email;
	}
	
	public String toString() {
		return "name: "+name+", email: "+email;
	}
	
	/**
	 * A contact is equal to another contact
	 * if the e-mail adresess are equal
	 */
	public boolean equals(Object o) {
		if(!(o instanceof Contact)) return false;
		return email.equals(((Contact)o).getEmailAddress());
	}
}
