package com.eteks.homeview3d.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;

public class HomeLight extends HomePieceOfFurniture implements Light {
  private static final long serialVersionUID = 1L;

  public enum Property {POWER};

  private final LightSource [] lightSources;
  private float power;

  private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  public HomeLight(Light light) {
    super(light);
    this.lightSources = light.getLightSources();
    this.power = 0.5f;
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    in.defaultReadObject();
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(listener);
    super.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(listener);
    super.removePropertyChangeListener(listener);
  }

  public LightSource [] getLightSources() {
    if (this.lightSources.length == 0) {
      return this.lightSources;
    } else {
      return this.lightSources.clone();
    }
  }

  public float getPower() {
    return this.power;
  }

  public void setPower(float power) {
    if (power != this.power) {
      float oldPower = this.power;
      this.power = power;
      this.propertyChangeSupport.firePropertyChange(Property.POWER.name(), oldPower, power);
    }
  }

  public HomeLight clone() {
    HomeLight clone = (HomeLight)super.clone();
    clone.propertyChangeSupport = new PropertyChangeSupport(clone);
    return clone;
  }
}
