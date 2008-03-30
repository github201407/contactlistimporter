package com.xdatasystem.contactsimporter;

import org.apache.http.cookie.CookieSpec;
import org.apache.http.cookie.CookieSpecFactory;
import org.apache.http.cookie.params.CookieSpecPNames;
import org.apache.http.params.HttpParams;

public class LenientCookieSpecFactory implements CookieSpecFactory {

  public CookieSpec newInstance(final HttpParams params) {
      if (params != null) {
          return new LenientCookieSpec(
                  (String []) params.getParameter(CookieSpecPNames.DATE_PATTERNS));
      } else {
          return new LenientCookieSpec();
      }
  }

}
