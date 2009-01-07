package com.xdatasystem.contactsimporter;

import com.xdatasystem.user.Contact;

/**
 * Contact implementation class.
 * 
 * @author Tjerk Wolterink
 * @author Thomas Bernard
 */
public class ContactImpl implements Contact {
	private String name;
	private String email;
	private String im;
	
	public ContactImpl(String name, String email) {
		this.name=name;
		this.email=email;
		this.im="";
	}
	
	public ContactImpl(String name, String email, String im) {
		this.name=name;
		this.email=email;
		this.im=im;
	}
	
	public String getName() {
		return name;
	}
	
	public String getGeneratedName() {
		if(name==null || name.length()==0) {
			return email.substring(0, email.indexOf('@'));
		} else {
			return name;
		}
	}

	public String getEmailAddress() {
		return email;
	}
	
	public String getIMAddress() {
		return im;
	}
	
	public String toString() {
		return 
			"name: "+name+
			", email: "+email+
			(im==null ? "" : ", im: "+im);
	}
	
	/**
	 * A contact is equal to another contact
	 * if the e-mail adress are equal
	 */
	public boolean equals(Object o) {
		if(!(o instanceof Contact)) return false;
		return email.equals(((Contact)o).getEmailAddress());
	}
}
