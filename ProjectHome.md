# _Contact List Importer_ 1.3.1 Java Library : Hotmail, Gmail & Yahoo #

## <font color='red'>Contact List Importer END OF LIVE</font> ##

In summary: <font color='red'>dont use this library anymore, there is a better way of importing contacts using the open API's of the different services.</font>

This software is deprecated as it uses html scraping in order to retrieve contacts. The API is itself an HTTP client and simply logs in to a service an gets the contacts. This method is prone to errors because the websites of the different contact-list providers change over time.

The better solution is to use the (new) open apis of the different versions. The contactlistimporter must be completely rewritten in order to work with those open apis. I do not have time for this.

Here is a (incomplete) list of pages with documentation for different open apis:

  * Hotmail: http://dev.live.com/contacts/
  * Gmail: http://googledataapis.blogspot.com/2008/03/3-2-1-contact-api-has-landed.html
  * Yahoo: http://developer.yahoo.com/social/contacts/ , http://developer.yahoo.com/addressbook/

If you want more apis just google them using _"serviceName contacts api"_.

I strongly advice NOT to use this software anymore.


**EVERYTHING BELOW THIS LINE IS DEPRECATED**

---


  * Version 1.3.1 is out: Support for the new hotmail version!
  * Thanks to Thomas Bernard!

## Introduction ##

Importing contacts from various services is not easy. The ContactListImporter provides an interface for retrieving contacts from various services.
Working implementations of that interface are provided for the following services.

  * Hotmail / Windows Live
  * Gmail
  * Yahoo

The implementation for hyves is broken due to a change in the hyves website.
It will  be fixed in the future.

## Please Donate ##

If you are using this project I have saved you either

  * Time and hard work
  * Money on licenses for other import software

So be kind a please donate. Any amount will do. I am a student
so i can really use the money.

[Click to Donate](http://www.wolterinkwebdesign.com/other/donate.html)

## Commercial Support ##

If you use this product in a commercial settings and need support or bug fixes on
can ask for commercial support. For more information on the cost please contact
the owner of this project (for mail address see below).

## Sample Code ##

Importing contacts from these services very easy with the ContactListImporter library. The following code imports all contacts and prints them to System.out:

```
import com.xdatasystem.contactsimporter.*;

// automatically guess the correct implementaion based on the email address
ContactListImporter importer=ContactListImporterFactory.guess("someuser@hotmail.com", "password");
List<Contact> contacts=importer.getContactList();
for(Contact c : contacts) {
  System.out.println("name: "+c.getName()+", email: "+c.getEmail());
}
```

Other ways to instantiate a ContactListImporter using the factory can be done as follows:

```
// creates an hotmail contact importer
importer=ContactListImporterFactory.hotmail("someuser@hotmail.com", "password");
// creates an gmail contact importer
importer=ContactListImporterFactory.gmail("someuser@gmail.com", "password");
```

## Dependencies ##

This library has the following depenencies:

  * The apache HttpComponents HttpClient 4.0 library: http://hc.apache.org/
    * Log4j
  * JSON library (needed for gmail importer) http://json.org/java
  * a CVS library for parsing comma-separated-formats

This library can be used to support a -invite-your-friends- function for your site.

This library builds upon the knowledge gathered by the creator of the following ruby contactsimporter library:

http://rufy.com/contacts/doc/

## Author ##

This program was created by Tjerk Wolterink and donated to the open source community.
Cristian Ventura added the Yahoo importer implementation.
You are free to add more importers, or to fix bugs.. just give me a mail: tjerkw _AT_ gmail.com  (replace _AT_ by the symbol, you know which symbol ;-)

Thanks to Thomas Bernard for the port to the new Hotmail Version.
Thanks to any other contributers & bug reports!