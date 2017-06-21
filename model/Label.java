package com.eteks.homeview3d.model;

import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;

public class Label extends HomeObject implements Selectable, Elevatable {
  private static final long serialVersionUID = 1L;
  
  private static final double TWICE_PI = 2 * Math.PI;

  public enum Property {TEXT, X, Y, ELEVATION, STYLE, COLOR, OUTLINE_COLOR, ANGLE, PITCH, LEVEL};
  
  private String              text;
  private float               x;
  private float               y;
  private TextStyle           style;
  private Integer             color;
  private Integer             outlineColor;
  private float               angle;
  private Float               pitch;
  private float               elevation;
  private Level               level;
  
  private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  public Label(String text, float x, float y) {
    this.text = text;
    this.x = x;
    this.y = y;
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    in.defaultReadObject();
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(listener);
  }

  public String getText() {
    return this.text;
  }

  public void setText(String text) {
    if (text != this.text
        && (text == null || !text.equals(this.text))) {
      String oldText = this.text;
      this.text = text;
      this.propertyChangeSupport.firePropertyChange(Property.TEXT.name(), oldText, text);
    }
  }

  public float getX() {
    return this.x;
  }

  public void setX(float x) {
    if (x != this.x) {
      float oldX = this.x;
      this.x = x;
      this.propertyChangeSupport.firePropertyChange(Property.X.name(), oldX, x);
    }
  }

  public float getY() {
    return this.y;
  }

  public void setY(float y) {
    if (y != this.y) {
      float oldY = this.y;
      this.y = y;
      this.propertyChangeSupport.firePropertyChange(Property.Y.name(), oldY, y);
    }
  }

  public float getGroundElevation() {
    if (this.level != null) {
      return this.elevation + this.level.getElevation();
    } else {
      return this.elevation;
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

  public TextStyle getStyle() {
    return this.style;  
  }

  public void setStyle(TextStyle style) {
    if (style != this.style) {
      TextStyle oldStyle = this.style;
      this.style = style;
      this.propertyChangeSupport.firePropertyChange(Property.STYLE.name(), oldStyle, style);
    }
  }

  public Integer getColor() {
    return this.color;  
  }

  public void setColor(Integer color) {
    if (color != this.color) {
      Integer oldColor = this.color;
      this.color = color;
      this.propertyChangeSupport.firePropertyChange(Property.COLOR.name(), oldColor, color);
    }
  }

  public Integer getOutlineColor() {
    return this.outlineColor;  
  }

  public void setOutlineColor(Integer outlineColor) {
    if (outlineColor != this.outlineColor) {
      Integer oldOutlineColor = this.outlineColor;
      this.outlineColor = outlineColor;
      this.propertyChangeSupport.firePropertyChange(Property.OUTLINE_COLOR.name(), oldOutlineColor, outlineColor);
    }
  }

  public float getAngle() {
    return this.angle;
  }

  public void setAngle(float angle) {
    angle = (float)((angle % TWICE_PI + TWICE_PI) % TWICE_PI);
    if (angle != this.angle) {
      float oldAngle = this.angle;
      this.angle = angle;
      this.propertyChangeSupport.firePropertyChange(Property.ANGLE.name(), oldAngle, angle);
    }
  }

  public Float getPitch() {
    return this.pitch;
  }

  public void setPitch(Float pitch) {
    if (pitch != null) {
      pitch = (float)((pitch % TWICE_PI + TWICE_PI) % TWICE_PI);
    }
    if (pitch != this.pitch
        && (pitch == null || !pitch.equals(this.pitch))) {
      Float oldPitch = this.pitch;
      this.pitch = pitch;
      this.propertyChangeSupport.firePropertyChange(Property.PITCH.name(), oldPitch, pitch);
    }
  }

  public Level getLevel() {
    return this.level;
  }

  public void setLevel(Level level) {
    if (level != this.level) {
      Level oldLevel = this.level;
      this.level = level;
      this.propertyChangeSupport.firePropertyChange(Property.LEVEL.name(), oldLevel, level);
    }
  }

  public boolean isAtLevel(Level level) {
    return this.level == level
        || this.level != null && level != null
           && this.level.getElevation() == level.getElevation()
           && this.level.getElevationIndex() < level.getElevationIndex();
  }

  public float [][] getPoints() {
    return new float [][] {{this.x, this.y}};
  }

  public boolean intersectsRectangle(float x0, float y0, 
                                     float x1, float y1) {
    Rectangle2D rectangle = new Rectangle2D.Float(x0, y0, 0, 0);
    rectangle.add(x1, y1);
    return rectangle.contains(this.x, this.y);
  }
 
  public boolean containsPoint(float x, float y, float margin) {
    return Math.abs(x - this.x) <= margin && Math.abs(y - this.y) <= margin;
  }

  public void move(float dx, float dy) {
    setX(getX() + dx);
    setY(getY() + dy);
  }

  public Label clone() {
    Label clone = (Label)super.clone();
    clone.propertyChangeSupport = new PropertyChangeSupport(clone);
    clone.level = null;
    return clone;
  }
}
