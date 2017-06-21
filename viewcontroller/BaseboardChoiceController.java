package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import com.eteks.homeview3d.model.Baseboard;
import com.eteks.homeview3d.model.UserPreferences;


public class BaseboardChoiceController implements Controller {
  public enum Property {VISIBLE, COLOR, PAINT, HEIGHT, MAX_HEIGHT, THICKNESS}
 
  public enum BaseboardPaint {DEFAULT, COLORED, TEXTURED} 

  private final UserPreferences       preferences;
  private final ViewFactory           viewFactory;
  private final ContentManager        contentManager;
  private TextureChoiceController     textureController;
  private final PropertyChangeSupport propertyChangeSupport;
  private View                        view;

  private Boolean   visible;
  private Float     thickness;
  private Float     height;
  private Float     maxHeight;
  private Integer   color;
  private BaseboardPaint paint;

  
  public BaseboardChoiceController(UserPreferences preferences, 
                                   ViewFactory viewFactory,
                                   ContentManager contentManager) {
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.contentManager = contentManager;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
  }

  public TextureChoiceController getTextureController() {
    if (this.textureController == null) {
      this.textureController = new TextureChoiceController(
          this.preferences.getLocalizedString(BaseboardChoiceController.class, "baseboardTextureTitle"), 
          this.preferences, this.viewFactory, this.contentManager);
      this.textureController.addPropertyChangeListener(TextureChoiceController.Property.TEXTURE,
          new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent ev) {
              setPaint(BaseboardPaint.TEXTURED);
            }
          });
    }
    return this.textureController;
  }

  
  public View getView() {
    if (this.view == null) {
      this.view = this.viewFactory.createBaseboardChoiceView(this.preferences, this); 
    }
    return this.view;
  }

 
  public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
  }

  
  public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
  }

  
  public Boolean getVisible() {
    return this.visible;
  }

  public void setVisible(Boolean baseboardVisible) {
    if (baseboardVisible != this.visible) {
      Boolean oldVisible = this.visible;
      this.visible = baseboardVisible;
      this.propertyChangeSupport.firePropertyChange(Property.VISIBLE.name(), 
          oldVisible, baseboardVisible);
    }
  }
 
  public void setThickness(Float baseboardThickness) {
    if (baseboardThickness != this.thickness) {
      Float oldThickness = this.thickness;
      this.thickness = baseboardThickness;
      this.propertyChangeSupport.firePropertyChange(Property.THICKNESS.name(), 
          oldThickness, baseboardThickness);
    }
  }
  
  
  public Float getThickness() {
    return this.thickness;
  }
  
 
  public void setHeight(Float baseboardHeight) {
    if (baseboardHeight != this.height) {
      Float oldHeight = this.height;
      this.height = baseboardHeight;
      this.propertyChangeSupport.firePropertyChange(Property.HEIGHT.name(), 
          oldHeight, baseboardHeight);
    }
  }
  
  public Float getHeight() {
    return this.height;
  }
  
  public void setMaxHeight(Float maxHeight) {
    if (this.maxHeight == null
        || maxHeight != this.maxHeight) {
      Float oldMaxHeight = this.maxHeight;
      this.maxHeight = maxHeight;
      this.propertyChangeSupport.firePropertyChange(Property.MAX_HEIGHT.name(), 
          oldMaxHeight, maxHeight);
    }
  }
  
  
  public Float getMaxHeight() {
    return this.maxHeight;
  }
  
  
  public void setColor(Integer baseboardColor) {
    if (baseboardColor != this.color) {
      Integer oldColor = this.color;
      this.color = baseboardColor;
      this.propertyChangeSupport.firePropertyChange(Property.COLOR.name(), oldColor, baseboardColor);
      
      setPaint(BaseboardPaint.COLORED);
    }
  }
  
  public Integer getColor() {
    return this.color;
  }

  
  public void setPaint(BaseboardPaint baseboardPaint) {
    if (baseboardPaint != this.paint) {
      BaseboardPaint oldPaint = this.paint;
      this.paint = baseboardPaint;
      this.propertyChangeSupport.firePropertyChange(Property.PAINT.name(), oldPaint, baseboardPaint);
    }
  }
  
  public BaseboardPaint getPaint() {
    return this.paint;
  }

  
  public void setBaseboard(Baseboard baseboard) {
    if (baseboard == null) {
      setVisible(false);
      setThickness(null);
      setHeight(null);
      setColor(null);
      getTextureController().setTexture(null);
      setPaint(null);
    } else {
      setVisible(true);
      setThickness(baseboard.getThickness());
      setHeight(baseboard.getHeight());
      if (baseboard.getTexture() != null) {
        setColor(null);
        getTextureController().setTexture(baseboard.getTexture());
        setPaint(BaseboardPaint.TEXTURED);
      } else if (baseboard.getColor() != null) {
        getTextureController().setTexture(null);
        setColor(baseboard.getColor());
      } else {
        setColor(null);
        getTextureController().setTexture(null);
        setPaint(BaseboardPaint.DEFAULT);
      }
    }
  }
}
