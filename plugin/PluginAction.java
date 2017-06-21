package com.eteks.homeview3d.plugin;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import com.eteks.homeview3d.tools.ResourceURLContent;

public abstract class PluginAction {
  /**
   * �� �׼��� ���� ������ �Ӽ� ����.
   */
  public enum Property {

    NAME,
  
    SHORT_DESCRIPTION,

    SMALL_ICON,

    MNEMONIC,

    TOOL_BAR,

    MENU,

    ENABLED;
  }
  
  private final Map<Property, Object> propertyValues        = new HashMap<Property, Object>();
  private final PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
  
  /**
   * ��Ȱ��ȭ �� �÷����� �۾� ����.
   */
  public PluginAction() {
  }


  public PluginAction(String resourceBaseName,
                      String actionPrefix,
                      ClassLoader pluginClassLoader) {
    this(resourceBaseName, actionPrefix, pluginClassLoader, false);
  }
  

  public PluginAction(String resourceBaseName, 
                      String actionPrefix,
                      ClassLoader pluginClassLoader, 
                      boolean enabled) {
    readActionPropertyValues(resourceBaseName, actionPrefix, pluginClassLoader);    
    setEnabled(enabled);
  }
    
  /**
   * ������ �⺻ �̸��� �ڿ����κ��� �Ӽ� �б�
   */
  private void readActionPropertyValues(String resourceBaseName, 
                                        String actionPrefix, 
                                        ClassLoader pluginClassLoader) {
    ResourceBundle resource;
    if (pluginClassLoader != null) {
      resource = ResourceBundle.getBundle(resourceBaseName, Locale.getDefault(), 
        pluginClassLoader);
    } else {
      resource = ResourceBundle.getBundle(resourceBaseName, Locale.getDefault());
    }
    String propertyPrefix = actionPrefix + ".";
    putPropertyValue(Property.NAME, 
        getOptionalString(resource, propertyPrefix + Property.NAME));
    putPropertyValue(Property.SHORT_DESCRIPTION, 
        getOptionalString(resource, propertyPrefix + Property.SHORT_DESCRIPTION));
    String smallIcon = getOptionalString(resource, propertyPrefix + Property.SMALL_ICON);
    if (smallIcon != null) {
      if (smallIcon.startsWith("/")) {
        smallIcon = smallIcon.substring(1);
      }
      putPropertyValue(Property.SMALL_ICON, 
          new ResourceURLContent(pluginClassLoader, smallIcon));
    }
    String mnemonicKey = getOptionalString(resource, propertyPrefix + Property.MNEMONIC);
    if (mnemonicKey != null) {
      putPropertyValue(Property.MNEMONIC, Character.valueOf(mnemonicKey.charAt(0)));
    }
    String toolBar = getOptionalString(resource, propertyPrefix + Property.TOOL_BAR);
    if (toolBar != null) {
      putPropertyValue(Property.TOOL_BAR, Boolean.valueOf(toolBar));
    }
    putPropertyValue(Property.MENU, 
        getOptionalString(resource, propertyPrefix + Property.MENU));
  }


  private String getOptionalString(ResourceBundle resource, String propertyKey) {
    try {
      return resource.getString(propertyKey);
    } catch (MissingResourceException ex) {
      return null;
    }
  }
  
  /**
   * �Ӽ� ��ȭ �߰�
   */
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(listener);
  }

  /**
   * �Ӽ� ��ȭ ����
   */
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(listener);
  }
  
  /**
   * �Ӽ� �� ��ȯ.
   */
  public Object getPropertyValue(Property property) {
    return this.propertyValues.get(property);
  }
  

  public void putPropertyValue(Property property, Object value) {
    Object oldValue = this.propertyValues.get(property);
    if (value != oldValue
        || (value != null && !value.equals(oldValue))) {
      this.propertyValues.put(property, value);
      this.propertyChangeSupport.firePropertyChange(property.name(), oldValue, value);
    }
    
  }


  public void setEnabled(boolean enabled) {
    putPropertyValue(Property.ENABLED, enabled);
  }
  

  public boolean isEnabled() {
    Boolean enabled = (Boolean)getPropertyValue(Property.ENABLED);
    return enabled != null && enabled.booleanValue();
  }
  
  /**
   * ����ڰ� ���� �� �׼� ����
   */
  public abstract void execute();
}
