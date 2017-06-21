package com.eteks.homeview3d.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public abstract class HomeObject implements Serializable, Cloneable {
  private static final long serialVersionUID = 1L;
  
  private Map<String, String> properties;

  public String getProperty(String name) {
    if (this.properties != null) {
      return this.properties.get(name);
    } else {
      return null;
    }
  }

  public void setProperty(String name, String value) {
    if (value == null) {
      if (this.properties != null && this.properties.containsKey(name)) {
        this.properties.remove(name);
        if (this.properties.size() == 0) {
          this.properties = null;
        }
      }
    } else {
      if (this.properties == null) {
        // º”º∫ ∏  ¿€º∫
        this.properties = Collections.singletonMap(name, value); 
      } else {
        if (this.properties.size() == 1) {
          this.properties = new HashMap<String, String>(this.properties);
        }
        this.properties.put(name, value);
      }
    }
  }

  public Collection<String> getPropertyNames() {
    if (this.properties != null) {
      return this.properties.keySet();
    } else {
      return Collections.emptySet();
    }
  }

  public HomeObject clone() {
    try {
      HomeObject clone = (HomeObject)super.clone();
      if (this.properties != null) {
        clone.properties = clone.properties.size() == 1 
            ? Collections.singletonMap(this.properties.keySet().iterator().next(), this.properties.values().iterator().next())
            : new HashMap<String, String>(this.properties);
      }
      return clone;
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException("Super class isn't cloneable"); 
    }
  }
}
