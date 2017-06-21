package com.eteks.homeview3d.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;

public class Level extends HomeObject {
  private static final long serialVersionUID = 1L;

  public enum Property {NAME, ELEVATION, HEIGHT, FLOOR_THICKNESS, BACKGROUND_IMAGE, VISIBLE, VIEWABLE, ELEVATION_INDEX};
      
  private String              name;
  private float               elevation;
  private float               floorThickness;
  private float               height;
  private BackgroundImage     backgroundImage;
  private boolean             visible;
  private boolean             viewable;
  private int                 elevationIndex;

  private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  public Level(String name, float elevation, float floorThickness, float height) {
    this.name = name;
    this.elevation = elevation;
    this.floorThickness = floorThickness;
    this.height = height;
    this.visible = true;
    this.viewable = true;
    this.elevationIndex = -1;
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    this.visible = true;
    this.viewable = true;
    this.elevationIndex = -1;
    in.defaultReadObject();
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(listener);
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    if (name != this.name
        && (name == null || !name.equals(this.name))) {
      String oldName = this.name;
      this.name = name;
      this.propertyChangeSupport.firePropertyChange(Property.NAME.name(), oldName, name);
    }
  }

  public float getElevation() {
    return this.elevation;
  }

  public void setElevation(float elevation) {
    if (elevation != this.elevation) {
      float oldElevation = this.elevation;
      this.elevation = elevation;
      this.propertyChangeSupport.firePropertyChange(Property.ELEVATION.name(), oldElevation, elevation);
    }
  }

  public float getFloorThickness() {
    return this.floorThickness;
  }

  public void setFloorThickness(float floorThickness) {
    if (floorThickness != this.floorThickness) {
      float oldFloorThickness = this.floorThickness;
      this.floorThickness = floorThickness;
      this.propertyChangeSupport.firePropertyChange(Property.FLOOR_THICKNESS.name(), oldFloorThickness, floorThickness);
    }
  }

  public float getHeight() {
    return this.height;
  }

  public void setHeight(float height) {
    if (height != this.height) {
      float oldHeight = this.height;
      this.height = height;
      this.propertyChangeSupport.firePropertyChange(Property.HEIGHT.name(), oldHeight, height);
    }
  }

  public BackgroundImage getBackgroundImage() {
    return this.backgroundImage;
  }

  public void setBackgroundImage(BackgroundImage backgroundImage) {
    if (backgroundImage != this.backgroundImage) {
      BackgroundImage oldBackgroundImage = this.backgroundImage;
      this.backgroundImage = backgroundImage;
      this.propertyChangeSupport.firePropertyChange(Property.BACKGROUND_IMAGE.name(), oldBackgroundImage, backgroundImage);
    }
  }

  public boolean isVisible() {
    return this.visible;
  }
  
  public void setVisible(boolean visible) {
    if (visible != this.visible) {
      this.visible = visible;
      this.propertyChangeSupport.firePropertyChange(Property.VISIBLE.name(), !visible, visible);
    }
  }

  public boolean isViewable() {
    return this.viewable;
  }

  public void setViewable(boolean viewable) {
    if (viewable != this.viewable) {
      this.viewable = viewable;
      this.propertyChangeSupport.firePropertyChange(Property.VIEWABLE.name(), !viewable, viewable);
    }
  }

  public boolean isViewableAndVisible() {
    return this.viewable && this.visible;
  }

  public int getElevationIndex() {
    return this.elevationIndex;
  }

  public void setElevationIndex(int elevationIndex) {
    if (elevationIndex != this.elevationIndex) {
      int oldElevationIndex = this.elevationIndex;
      this.elevationIndex = elevationIndex;
      this.propertyChangeSupport.firePropertyChange(Property.ELEVATION_INDEX.name(), oldElevationIndex, elevationIndex);
    }
  }

  public Level clone() {
    Level clone = (Level)super.clone();
    clone.propertyChangeSupport = new PropertyChangeSupport(clone);
    return clone;
  }
}
