package com.xdatasystem.contactsimporter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLConnection;

public class HttpUtils {
	/**
	 * gets the text content of the connection
	 * 
	 * @param con
	 *          an open url connection
	 * @return
	 * @throws IOException 
	 */
	public static String getTextContent(URLConnection urlConn) throws IOException {
		BufferedReader htmlPage=new BufferedReader(
			new InputStreamReader(
					urlConn.getInputStream()
			)
		); 
		
		StringBuffer contents=new StringBuffer();
		
		String line = ""; 
		while((line=htmlPage.readLine())!=null) {  
			contents.append(line);
		}  
		htmlPage.close();
		return contents.toString();
	}
}
