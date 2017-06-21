package com.eteks.homeview3d.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

import com.eteks.homeview3d.tools.URLContent;

class URLContentClassLoader extends ClassLoader {
  private final URL url;

  public URLContentClassLoader(URL url) {
    this.url = url;
  }

  @Override
  public InputStream getResourceAsStream(String name) {
    try {
      return new URLContent(new URL("jar:" + this.url.toURI() + "!/" + name)).openStream();
    } catch (IOException ex) {
      return null;
    } catch (URISyntaxException ex) {
      return null;
    }
  }
}