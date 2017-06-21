package com.eteks.homeview3d.model;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;

public class ObserverCamera extends Camera implements Selectable {

  public enum Property {WIDTH, DEPTH, HEIGHT}
  
  private static final long serialVersionUID = 1L;

  private boolean fixedSize;
  
  private transient Shape shapeCache;
  private transient Shape rectangleShapeCache;

  private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  public ObserverCamera(float x, float y, float z, float yaw, float pitch, float fieldOfView) {
    super(x, y, z, yaw, pitch, fieldOfView);
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

  public void setFixedSize(boolean fixedSize) {
    if (this.fixedSize != fixedSize) {
      float oldWidth = getWidth();
      float oldDepth = getDepth();
      float oldHeight = getHeight();
      this.fixedSize = fixedSize;
      this.shapeCache = null;
      this.rectangleShapeCache = null;
      this.propertyChangeSupport.firePropertyChange(Property.WIDTH.name(), oldWidth, getWidth());
      this.propertyChangeSupport.firePropertyChange(Property.DEPTH.name(), oldDepth, getDepth());
      this.propertyChangeSupport.firePropertyChange(Property.HEIGHT.name(), oldHeight, getHeight());
    }
  }

  public boolean isFixedSize() {
    return this.fixedSize;
  }

  public void setYaw(float yaw) {
    super.setYaw(yaw);
    this.shapeCache = null;
    this.rectangleShapeCache = null;
  }

  public void setX(float x) {
    super.setX(x);
    this.shapeCache = null;
    this.rectangleShapeCache = null;
  }

  public void setY(float y) {
    super.setY(y);
    this.shapeCache = null;
    this.rectangleShapeCache = null;
  }

  public void setZ(float z) {
    float oldWidth = getWidth();
    float oldDepth = getDepth();
    float oldHeight = getHeight();
    super.setZ(z);
    this.shapeCache = null;
    this.rectangleShapeCache = null;
    this.propertyChangeSupport.firePropertyChange(Property.WIDTH.name(), oldWidth, getWidth());
    this.propertyChangeSupport.firePropertyChange(Property.DEPTH.name(), oldDepth, getDepth());
    this.propertyChangeSupport.firePropertyChange(Property.HEIGHT.name(), oldHeight, getHeight());
  }

  public float getWidth() {
    if (this.fixedSize) {
      return 46.6f;
    } else {  
      float width = getZ() * 4 / 14;
      return Math.min(Math.max(width, 20), 62.5f);
    }
  }

  public float getDepth() {
    if (this.fixedSize) {
      return 18.6f;
    } else {
      float depth = getZ() * 8 / 70;
      return Math.min(Math.max(depth, 8), 25);
    }
  }

  public float getHeight() {
    if (this.fixedSize) {
      return 175f;
    } else {
      return getZ() * 15 / 14;
    }
  }

  public float [][] getPoints() {
    float [][] cameraPoints = new float[4][2];
    PathIterator it = getRectangleShape().getPathIterator(null);
    for (int i = 0; i < cameraPoints.length; i++) {
      it.currentSegment(cameraPoints [i]);
      it.next();
    }
    return cameraPoints;
  }

  public boolean intersectsRectangle(float x0, float y0, 
                                     float x1, float y1) {
    Rectangle2D rectangle = new Rectangle2D.Float(x0, y0, 0, 0);
    rectangle.add(x1, y1);
    return getShape().intersects(rectangle);
  }

  public boolean containsPoint(float x, float y, float margin) {
    if (margin == 0) {
      return getShape().contains(x, y);
    } else {
      return getShape().intersects(x - margin, y - margin, 2 * margin, 2 * margin);
    }
  }

  private Shape getShape() {
    if (this.shapeCache == null) {
      Ellipse2D cameraEllipse = new Ellipse2D.Float(
          getX() - getWidth() / 2, getY() - getDepth() / 2,
          getWidth(), getDepth());
      AffineTransform rotation = AffineTransform.getRotateInstance(getYaw(), getX(), getY());
      PathIterator it = cameraEllipse.getPathIterator(rotation);
      GeneralPath pieceShape = new GeneralPath();
      pieceShape.append(it, false);
      this.shapeCache = pieceShape;
    }
    return this.shapeCache;
  }

  private Shape getRectangleShape() {
    if (this.rectangleShapeCache == null) {
      Rectangle2D cameraRectangle = new Rectangle2D.Float(
          getX() - getWidth() / 2, getY() - getDepth() / 2,
          getWidth(), getDepth());
      AffineTransform rotation = AffineTransform.getRotateInstance(getYaw(), getX(), getY());
      PathIterator it = cameraRectangle.getPathIterator(rotation);
      GeneralPath cameraRectangleShape = new GeneralPath();
      cameraRectangleShape.append(it, false);
      this.rectangleShapeCache = cameraRectangleShape;
    }
    return this.rectangleShapeCache;
  }

  public void move(float dx, float dy) {
    setX(getX() + dx);
    setY(getY() + dy);
  }

  public ObserverCamera clone() {
    return (ObserverCamera)super.clone();
  }
}
