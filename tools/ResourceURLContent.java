package com.eteks.homeview3d.tools;

import java.net.MalformedURLException;
import java.net.URL;


public class ResourceURLContent extends URLContent {
  private static final long serialVersionUID = 1L;

  private boolean multiPartResource;

  public ResourceURLContent(Class<?> resourceClass, 
                            String resourceName) {
    this(resourceClass, resourceName, false);
  }

  public ResourceURLContent(Class<?> resourceClass,
                            String resourceName, 
                            boolean multiPartResource) {
    super(getClassResource(resourceClass, resourceName));
    if (getURL() == null) {
      throw new IllegalArgumentException("Unknown resource " + resourceName);
    }
    this.multiPartResource = multiPartResource;
  }

  public ResourceURLContent(ClassLoader resourceClassLoader, 
                            String resourceName) {
    super(resourceClassLoader.getResource(resourceName));
    if (getURL() == null) {
      throw new IllegalArgumentException("Unknown resource " + resourceName);
    }
  }

  private static final boolean isJava1dot5dot0_16 = 
      System.getProperty("java.version").startsWith("1.5.0_16"); 

  private static URL getClassResource(Class<?> resourceClass,
                                      String resourceName) {
    URL defaultUrl = resourceClass.getResource(resourceName);

    if (isJava1dot5dot0_16
        && defaultUrl != null
        && "jar".equalsIgnoreCase(defaultUrl.getProtocol())) {
      String defaultUrlExternalForm = defaultUrl.toExternalForm();
      if (defaultUrl.toExternalForm().indexOf("!/") == -1) {
        String fixedUrl = "jar:" 
          + resourceClass.getProtectionDomain().getCodeSource().getLocation().toExternalForm() 
          + "!/" + defaultUrl.getPath();
        
        if (!fixedUrl.equals(defaultUrlExternalForm)) {
          try {
            return new URL(fixedUrl);
          } catch (MalformedURLException ex) {
          } 
        }
      }
    }
    return defaultUrl;
  }

  public ResourceURLContent(URL url, boolean multiPartResource) {
    super(url);
    this.multiPartResource = multiPartResource;
  }

  public boolean isMultiPartResource() {
    return this.multiPartResource;
  }
}
