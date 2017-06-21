package com.eteks.homeview3d.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;

import com.eteks.homeview3d.model.Content;

public class URLContent implements Content {
  private static final long serialVersionUID = 1L;

  private URL url;
  
  public URLContent(URL url) {
    this.url = url;
  }

  /**
   * URL 반환.
   */
  public URL getURL() {
    return this.url;
  }

  public InputStream openStream() throws IOException {
    URLConnection connection = getURL().openConnection();
    if (OperatingSystem.isWindows() && isJAREntry()) {
      URL jarEntryURL = getJAREntryURL();
      if (jarEntryURL.getProtocol().equalsIgnoreCase("file")) {
        try {
          if (new File(jarEntryURL.toURI()).canWrite()) {
            connection.setUseCaches(false);
          }
        } catch (URISyntaxException ex) {
          IOException ex2 = new IOException();
          ex2.initCause(ex);
          throw ex2;
        }
      }
    }
    return connection.getInputStream();
  }

  public boolean isJAREntry() {
    return "jar".equals(this.url.getProtocol());
  }

  public URL getJAREntryURL() {
    if (!isJAREntry()) {
      throw new IllegalStateException("Content isn't a JAR entry");
    }
    try {
      String file = this.url.getFile();
      return new URL(file.substring(0, file.indexOf('!')));
    } catch (MalformedURLException ex) {
      throw new IllegalStateException("Invalid URL base for JAR entry", ex);
    }
  }

  /**
   * JAR 엔트리 이름 반환. 
   */
  public String getJAREntryName() {
    if (!isJAREntry()) {
      throw new IllegalStateException("Content isn't a JAR entry");
    }
    String file = this.url.getFile();
    return file.substring(file.indexOf('!') + 2);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof URLContent) {
      URLContent urlContent = (URLContent)obj;
      return urlContent.url == this.url
          || urlContent.url.equals(this.url);
    } else {
      return false;
    }
  }
  
  @Override
  public int hashCode() {
    return this.url.hashCode();
  }
}
