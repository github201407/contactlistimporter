package com.xdatasystem.user;

public interface Contact {
	
	/**
	 * Gets the name of this user. This value
	 * is normally used to display this user in the user interface.
	 *
	 * @return the name of this user
	 * @ensure return!=null
	 */
	public String getName();
	
	/**
	 * Gets the e-mail address of the user
	 */
	public String getEmailAddress();
}
