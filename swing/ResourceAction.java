package com.eteks.homeview3d.swing;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.KeyStroke;
import javax.swing.event.SwingPropertyChangeSupport;

import com.eteks.homeview3d.model.UserPreferences;
import com.eteks.homeview3d.tools.OperatingSystem;

public class ResourceAction extends AbstractAction {
  public static final String POPUP = "Popup";
  public static final String TOGGLE_BUTTON_MODEL = "ToggleButtonModel";

  public ResourceAction(UserPreferences preferences, 
                        Class<?> resourceClass, 
                        String actionPrefix) {
    this(preferences, resourceClass, actionPrefix, false);
  }
  

  public ResourceAction(UserPreferences preferences, 
                        Class<?> resourceClass, 
                        String actionPrefix, 
                        boolean enabled) {
    readActionProperties(preferences, resourceClass, actionPrefix);    
    setEnabled(enabled);
    
    preferences.addPropertyChangeListener(UserPreferences.Property.LANGUAGE, 
        new LanguageChangeListener(this, resourceClass, actionPrefix));
  }
 
  private static class LanguageChangeListener implements PropertyChangeListener {
    private final WeakReference<ResourceAction> resourceAction;
    private final Class<?>                      resourceClass;
    private final String                        actionPrefix;

    public LanguageChangeListener(ResourceAction resourceAction,
                                  Class<?> resourceClass,
                                  String actionPrefix) {
      this.resourceAction = new WeakReference<ResourceAction>(resourceAction);
      this.resourceClass = resourceClass;
      this.actionPrefix = actionPrefix;
    }

    public void propertyChange(PropertyChangeEvent ev) {
      ResourceAction resourceAction = this.resourceAction.get();
      if (resourceAction == null) {
        ((UserPreferences)ev.getSource()).removePropertyChangeListener(
            UserPreferences.Property.LANGUAGE, this);
      } else {
        resourceAction.readActionProperties((UserPreferences)ev.getSource(), 
            this.resourceClass, this.actionPrefix);
      }
    }
  }

  private void readActionProperties(UserPreferences preferences, 
                                    Class<?> resourceClass, 
                                    String actionPrefix) {
    String propertyPrefix = actionPrefix + ".";
    putValue(NAME, getOptionalString(preferences, resourceClass, propertyPrefix + NAME, true));
    putValue(DEFAULT, getValue(NAME));
    putValue(POPUP, getOptionalString(preferences, resourceClass, propertyPrefix + POPUP, true));
    
    putValue(SHORT_DESCRIPTION, 
        getOptionalString(preferences, resourceClass, propertyPrefix + SHORT_DESCRIPTION, false));
    putValue(LONG_DESCRIPTION, 
        getOptionalString(preferences, resourceClass, propertyPrefix + LONG_DESCRIPTION, false));
    
    String smallIcon = getOptionalString(preferences, resourceClass, propertyPrefix + SMALL_ICON, false);
    if (smallIcon != null) {
      putValue(SMALL_ICON, SwingTools.getScaledImageIcon(resourceClass.getResource(smallIcon)));
    }

    String propertyKey = propertyPrefix + ACCELERATOR_KEY;
    String acceleratorKey = getOptionalString(preferences, 
        resourceClass, propertyKey + "." + System.getProperty("os.name"), false);
    if (acceleratorKey == null) {
      acceleratorKey = getOptionalString(preferences, resourceClass, propertyKey, false);
    }
    if (acceleratorKey !=  null) {
      putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(acceleratorKey));
    }
    
    String mnemonicKey = getOptionalString(preferences, resourceClass, propertyPrefix + MNEMONIC_KEY, false);
    if (mnemonicKey != null) {
      putValue(MNEMONIC_KEY, Integer.valueOf(KeyStroke.getKeyStroke(mnemonicKey).getKeyCode()));
    }
  }

  private String getOptionalString(UserPreferences preferences, 
                                   Class<?> resourceClass, 
                                   String propertyKey,
                                   boolean label) {
    try {
      String localizedText = label 
          ? SwingTools.getLocalizedLabelText(preferences, resourceClass, propertyKey)
          : preferences.getLocalizedString(resourceClass, propertyKey);
      if (localizedText != null && localizedText.length() > 0) {
        return localizedText;
      } else {
        return null;
      }
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  public void actionPerformed(ActionEvent ev) {
    throw new UnsupportedOperationException();
  }
  
  private static class AbstractDecoratedAction implements Action {
    private Action action;
    private SwingPropertyChangeSupport propertyChangeSupport;

    public AbstractDecoratedAction(Action action) {
      this.action = action;
      this.propertyChangeSupport = new SwingPropertyChangeSupport(this);
      action.addPropertyChangeListener(new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            String propertyName = ev.getPropertyName();
            if ("enabled".equals(propertyName)) {
              propertyChangeSupport.firePropertyChange(ev);
            } else {
              Object newValue = getValue(propertyName);
              if (newValue != null) {
                propertyChangeSupport.firePropertyChange(new PropertyChangeEvent(ev.getSource(), 
                    propertyName, ev.getOldValue(), newValue));
              }
            }
          }
        });
    }

    public final void actionPerformed(ActionEvent ev) {
      this.action.actionPerformed(ev);
    }

    public final void addPropertyChangeListener(PropertyChangeListener listener) {
      this.propertyChangeSupport.addPropertyChangeListener(listener);
    }

    public Object getValue(String key) {
      return this.action.getValue(key);
    }

    public final boolean isEnabled() {
      return this.action.isEnabled();
    }

    public final void putValue(String key, Object value) {
      this.action.putValue(key, value);
    }

    public final void removePropertyChangeListener(PropertyChangeListener listener) {
      this.propertyChangeSupport.removePropertyChangeListener(listener);
    }

    public final void setEnabled(boolean enabled) {
      this.action.setEnabled(enabled);
    }
    
    protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
      this.propertyChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
    }
  }
  
  public static class MenuItemAction extends AbstractDecoratedAction {
    public MenuItemAction(Action action) {
      super(action);
    }

    public Object getValue(String key) {
      if (OperatingSystem.isMacOSX()
          && (key.equals(MNEMONIC_KEY)
              || key.equals(SMALL_ICON)
              || key.equals(SHORT_DESCRIPTION))) {
        return null;
      }
      return super.getValue(key);
    }
  }
  

  public static class PopupMenuItemAction extends MenuItemAction {
    public PopupMenuItemAction(Action action) {
      super(action);
      addPropertyChangeListener(new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent ev) {
            if (POPUP.equals(ev.getPropertyName())
                && (ev.getOldValue() != null || ev.getNewValue() != null)) {
              firePropertyChange(NAME, ev.getOldValue(), ev.getNewValue());
            }
          }
        });
    }

    public Object getValue(String key) {
      if (key.equals(NAME)) {
        Object value = super.getValue(POPUP);
        if (value != null) {
          return value;
        }
      } else if (key.equals(SMALL_ICON)) {
        // Avoid icons in popus
        return null;
      } else if (OperatingSystem.isMacOSX()
                 && key.equals(ACCELERATOR_KEY)) {
        return null;
      }
      return super.getValue(key);
    }
  }

  public static class ToolBarAction extends AbstractDecoratedAction {
    public ToolBarAction(Action action) {
      super(action);
    }

    public Object getValue(String key) {
      if (key.equals(NAME)) {        
        return null;
      } 
      return super.getValue(key);
    }
  }

  public static class ButtonAction extends AbstractDecoratedAction {
    public ButtonAction(Action action) {
      super(action);
    }

    public Object getValue(String key) {
      if (OperatingSystem.isMacOSX()
          && key.equals(MNEMONIC_KEY)) {
        return null;
      }
      return super.getValue(key);
    }
  }
}
