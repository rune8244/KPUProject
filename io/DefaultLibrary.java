package com.eteks.homeview3d.io;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.eteks.homeview3d.model.Library;

class DefaultLibrary implements Library {
  private static final String ID          = "id"; 
  private static final String NAME        = "name"; 
  private static final String DESCRIPTION = "description"; 
  private static final String VERSION     = "version"; 
  private static final String LICENSE     = "license"; 
  private static final String PROVIDER    = "provider"; 
  
  private final String location;
  private final String type;
  private final String id;
  private final String name;
  private final String description;
  private final String version;
  private final String license; 
  private final String provider;

  /**
   * 라이브러리 이니셜라이즈.
   */
  public DefaultLibrary(String location, String type,  
                        String id, String name, String description, String version, String license,
                        String provider) {
    this.location = location;
    this.type = type;
    this.id = id;
    this.name = name;
    this.description = description;
    this.version = version;
    this.license = license;
    this.provider = provider;
  }
  
  public DefaultLibrary(String location, String type, ResourceBundle resource) {
    this.location = location;
    this.type = type;
    this.id = getOptionalString(resource, ID);
    this.name = getOptionalString(resource, NAME);
    this.description = getOptionalString(resource, DESCRIPTION);
    this.version = getOptionalString(resource, VERSION);
    this.license = getOptionalString(resource, LICENSE);
    this.provider = getOptionalString(resource, PROVIDER);
  }

  private String getOptionalString(ResourceBundle resource, String propertyKey) {
    try {
      return resource.getString(propertyKey);
    } catch (MissingResourceException ex) {
      return null;
    }
  }

  /**
   * 라이브러리가 저장된 장소로 되돌아감.
   */
  public String getLocation() {
    return this.location;
  }
  
  /**
   * 이 라이브러리 id반납.
   */
  public String getId() {
    return this.id;
  }
  
  public String getType() {
    return this.type;
  }
  
  /**
   * 라이브러리 이름 반납.
   */
  public String getName() {
    return this.name;
  }

  public String getDescription() {
    return this.description;
  }

  public String getVersion() {
    return this.version;
  }

  public String getLicense() {
    return this.license;
  }


  public String getProvider() {
    return this.provider;
  }
}
