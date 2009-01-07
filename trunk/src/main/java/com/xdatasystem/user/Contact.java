package com.xdatasystem.user;

/**
 * Simple representation for a contact.
 * A contact has a name and an email and optionally an IMAddress;
 * 
 * @author Tjerk Wolterink
 * @author Thomas Bernard
 */
public interface Contact {
	
	/**
	 * Returns the name of the contact.
	 * Note that some contactImporters cannot
	 * retrieve the name: in that case the
	 * name may be "" or even the empty string.
	 * 
	 * Use getGeneratedName() if you realy need
	 * a name. A name is generated if none found.
	 * 
	 * @return the name of the contact
	 */
	public String getName();
	
	/**
	 * Returns the name of the contact.
	 * Or it generated a name from the email address
	 * if no name was found.
	 * 
	 * Use this method if you really need a name but 
	 * you are not sure if the contact has a name.
	 * 
	 * Note that there is no guarantee that the name is unique.
	 * 
	 * @return a string that can be used as the name for this contact
	 */
	public String getGeneratedName();
	
	/**
	 * Gets the e-mail address of the user
	 * @return the email address of this user
	 */
	public String getEmailAddress();
	
	/**
	 * Gets the IM identifier of this user.
	 * Returns null if no IMAdresss was set.
	 * 
	 * @return the IM identifier of this user or null
	 */
	public String getIMAddress();
}
