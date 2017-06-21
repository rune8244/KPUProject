package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.ref.WeakReference;

import com.eteks.homeview3d.model.AspectRatio;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeEnvironment;
import com.eteks.homeview3d.model.UserPreferences;

public abstract class AbstractPhotoController implements Controller {
  public enum Property {ASPECT_RATIO, WIDTH, HEIGHT, QUALITY, VIEW_3D_ASPECT_RATIO, CEILING_LIGHT_COLOR}
  
  private final Home                  home;
  private final View                  view3D;
  private final ContentManager        contentManager;
  private final PropertyChangeSupport propertyChangeSupport;
  
  private AspectRatio                 aspectRatio;
  private int                         width;
  private int                         height;
  private int                         quality;
  private float                       view3DAspectRatio;
  private int                         ceilingLightColor;

  public AbstractPhotoController(Home home,
                                 UserPreferences preferences,
                                 View view3D,
                                 ContentManager contentManager) {
    this.home = home;
    this.view3D = view3D;
    this.contentManager = contentManager;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    this.view3DAspectRatio = 1;
    
    EnvironmentChangeListener listener = new EnvironmentChangeListener(this);
    home.getEnvironment().addPropertyChangeListener(HomeEnvironment.Property.PHOTO_WIDTH, listener);
    home.getEnvironment().addPropertyChangeListener(HomeEnvironment.Property.PHOTO_HEIGHT, listener);
    home.getEnvironment().addPropertyChangeListener(HomeEnvironment.Property.PHOTO_ASPECT_RATIO, listener);
    home.getEnvironment().addPropertyChangeListener(HomeEnvironment.Property.PHOTO_QUALITY, listener);
    home.getEnvironment().addPropertyChangeListener(HomeEnvironment.Property.CEILING_LIGHT_COLOR, listener);
    updateProperties();
  }

  
  private static class EnvironmentChangeListener implements PropertyChangeListener {
    private WeakReference<AbstractPhotoController> photoController;
    
    public EnvironmentChangeListener(AbstractPhotoController photoController) {
      this.photoController = new WeakReference<AbstractPhotoController>(photoController);
    }
    
    public void propertyChange(PropertyChangeEvent ev) {
      final AbstractPhotoController controller = this.photoController.get();
      if (controller == null) {
        ((HomeEnvironment)ev.getSource()).removePropertyChangeListener(HomeEnvironment.Property.PHOTO_WIDTH, this);
        ((HomeEnvironment)ev.getSource()).removePropertyChangeListener(HomeEnvironment.Property.PHOTO_HEIGHT, this);
        ((HomeEnvironment)ev.getSource()).removePropertyChangeListener(HomeEnvironment.Property.PHOTO_ASPECT_RATIO, this);
        ((HomeEnvironment)ev.getSource()).removePropertyChangeListener(HomeEnvironment.Property.PHOTO_QUALITY, this);
        ((HomeEnvironment)ev.getSource()).removePropertyChangeListener(HomeEnvironment.Property.CEILING_LIGHT_COLOR, this);
      } else if (HomeEnvironment.Property.PHOTO_WIDTH.name().equals(ev.getPropertyName())) {
        controller.setWidth((Integer)ev.getNewValue(), false);
      } else if (HomeEnvironment.Property.PHOTO_HEIGHT.name().equals(ev.getPropertyName())) {
        controller.setHeight((Integer)ev.getNewValue(), false);
      } else if (HomeEnvironment.Property.PHOTO_ASPECT_RATIO.name().equals(ev.getPropertyName())) {
        controller.setAspectRatio((AspectRatio)ev.getNewValue());
      } else if (HomeEnvironment.Property.PHOTO_QUALITY.name().equals(ev.getPropertyName())) {
        controller.setQuality((Integer)ev.getNewValue());
      } else if (HomeEnvironment.Property.CEILING_LIGHT_COLOR.name().equals(ev.getPropertyName())) {
        controller.setCeilingLightColor((Integer)ev.getNewValue());
      }
    }
  }

  public ContentManager getContentManager() {
    return this.contentManager;
  }

  
  public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
  }

  
  public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
  }

  
  protected void updateProperties() {
    HomeEnvironment homeEnvironment = this.home.getEnvironment();
    setAspectRatio(homeEnvironment.getPhotoAspectRatio());
    setWidth(homeEnvironment.getPhotoWidth(), false);
    setHeight(homeEnvironment.getPhotoHeight(), false);
    setQuality(homeEnvironment.getPhotoQuality());
    setCeilingLightColor(homeEnvironment.getCeillingLightColor());
  }
  
  
  public void setAspectRatio(AspectRatio aspectRatio) {
    if (this.aspectRatio != aspectRatio) {
      AspectRatio oldAspectRatio = this.aspectRatio;
      this.aspectRatio = aspectRatio;
      this.propertyChangeSupport.firePropertyChange(Property.ASPECT_RATIO.name(), oldAspectRatio, aspectRatio);
      this.home.getEnvironment().setPhotoAspectRatio(this.aspectRatio);
      if (this.aspectRatio == AspectRatio.VIEW_3D_RATIO) {
        if (this.view3DAspectRatio != Float.POSITIVE_INFINITY) {
          setHeight(Math.round(width / this.view3DAspectRatio), false);
        }
      } else if (this.aspectRatio.getValue() != null) {
        setHeight(Math.round(width / this.aspectRatio.getValue()), false);
      }
    }
  }
 
  public AspectRatio getAspectRatio() {
    return this.aspectRatio;
  }

  
  public void setWidth(int width) {
    setWidth(width, true);
  }
  
  private void setWidth(int width, boolean updateHeight) {
    if (this.width != width) {
      int oldWidth = this.width;
      this.width = width;
      this.propertyChangeSupport.firePropertyChange(Property.WIDTH.name(), oldWidth, width);
      if (updateHeight) {
        if (this.aspectRatio == AspectRatio.VIEW_3D_RATIO) {
          if (this.view3DAspectRatio != Float.POSITIVE_INFINITY) {
            setHeight(Math.round(width / this.view3DAspectRatio), false);
          }
        } else if (this.aspectRatio.getValue() != null) {
          setHeight(Math.round(width / this.aspectRatio.getValue()), false);
        }
      }
      this.home.getEnvironment().setPhotoWidth(this.width);
    }
  }
  
 
  public int getWidth() {
    return this.width;
  }

  public void setHeight(int height) {
    setHeight(height, true);
  }
  
  private void setHeight(int height, boolean updateWidth) {
    if (this.height != height) {
      int oldHeight = this.height;
      this.height = height;
      this.propertyChangeSupport.firePropertyChange(Property.HEIGHT.name(), oldHeight, height);
      if (updateWidth) {
        if (this.aspectRatio == AspectRatio.VIEW_3D_RATIO) {
          if (this.view3DAspectRatio != Float.POSITIVE_INFINITY) {
            setWidth(Math.round(height * this.view3DAspectRatio), false);
          }
        } else if (this.aspectRatio.getValue() != null) {
          setWidth(Math.round(height * this.aspectRatio.getValue()), false);
        }
      }
      this.home.getEnvironment().setPhotoHeight(this.height);
    }
  }
  
  
  public int getHeight() {
    return this.height;
  }

 
  public void setQuality(int quality) {
    if (this.quality != quality) {
      int oldQuality = this.quality;
      this.quality = Math.min(quality, getQualityLevelCount() - 1);
      this.propertyChangeSupport.firePropertyChange(Property.QUALITY.name(), oldQuality, quality);
      this.home.getEnvironment().setPhotoQuality(this.quality);
    }
  }
  

  public int getQuality() {
    return this.quality;
  }

  public int getQualityLevelCount() {
    return 4;
  }
  
 
  public void setCeilingLightColor(int ceilingLightColor) {
    if (this.ceilingLightColor != ceilingLightColor) {
      int oldCeilingLightColor = this.ceilingLightColor;
      this.ceilingLightColor = ceilingLightColor;
      this.propertyChangeSupport.firePropertyChange(Property.CEILING_LIGHT_COLOR.name(), oldCeilingLightColor, ceilingLightColor);
      this.home.getEnvironment().setCeillingLightColor(ceilingLightColor);
    }
  }
  

  public int getCeilingLightColor() {
    return this.ceilingLightColor;
  }

  
  public void set3DViewAspectRatio(float view3DAspectRatio) {
    if (this.view3DAspectRatio != view3DAspectRatio) {
      float oldAspectRatio = this.view3DAspectRatio;
      this.view3DAspectRatio = view3DAspectRatio;
      this.propertyChangeSupport.firePropertyChange(Property.ASPECT_RATIO.name(), oldAspectRatio, view3DAspectRatio);
      if (this.aspectRatio == AspectRatio.VIEW_3D_RATIO
          && this.view3DAspectRatio != Float.POSITIVE_INFINITY) {
        setHeight(Math.round(this.width / this.view3DAspectRatio), false);
      }
    }
  }
  
  
  public float get3DViewAspectRatio() {
    return this.view3DAspectRatio;
  }

  
  public View get3DView() {
    return this.view3D;
  }

 
  public void setVisualProperty(String propertyName,
                                Object propertyValue) {
    this.home.setVisualProperty(propertyName, propertyValue);
  }

  public void setHomeProperty(String propertyName,
                                String propertyValue) {
    this.home.setProperty(propertyName, propertyValue);
  }
}
