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
   * 이 액션이 정의 가능한 속성 나열.
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
   * 비활성화 된 플러그인 작업 생성.
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
   * 지정된 기본 이름의 자원으로부터 속성 읽기
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
   * 속성 변화 추가
   */
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(listener);
  }

  /**
   * 속성 변화 제거
   */
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(listener);
  }
  
  /**
   * 속성 값 반환.
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
   * 사용자가 원할 때 액션 실행
   */
  public abstract void execute();
}
