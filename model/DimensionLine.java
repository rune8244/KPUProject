package com.eteks.homeview3d.model;

import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;

public class DimensionLine extends HomeObject implements Selectable, Elevatable {
  public enum Property {X_START, Y_START, X_END, Y_END, OFFSET, LENGTH_STYLE, LEVEL} 
   
  private static final long serialVersionUID = 1L;
  
  private float               xStart;
  private float               yStart;
  private float               xEnd;
  private float               yEnd;
  private float               offset;
  private TextStyle           lengthStyle;
  private Level               level;

  private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
  private transient Shape shapeCache;

  public DimensionLine(float xStart, float yStart, float xEnd, float yEnd, float offset) {
    this.xStart = xStart;
    this.yStart = yStart;
    this.xEnd = xEnd;
    this.yEnd = yEnd;
    this.offset = offset;
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

  public float getXStart() {
    return this.xStart;
  }

  public void setXStart(float xStart) {
    if (xStart != this.xStart) {
      float oldXStart = this.xStart;
      this.xStart = xStart;
      this.shapeCache = null;
      this.propertyChangeSupport.firePropertyChange(Property.X_START.name(), oldXStart, xStart);
    }
  }

  public float getYStart() {
    return this.yStart;
  }

  public void setYStart(float yStart) {
    if (yStart != this.yStart) {
      float oldYStart = this.yStart;
      this.yStart = yStart;
      this.shapeCache = null;
      this.propertyChangeSupport.firePropertyChange(Property.Y_START.name(), oldYStart, yStart);
    }
  }

  public float getXEnd() {
    return this.xEnd;
  }

  public void setXEnd(float xEnd) {
    if (xEnd != this.xEnd) {
      float oldXEnd = this.xEnd;
      this.xEnd = xEnd;
      this.shapeCache = null;
      this.propertyChangeSupport.firePropertyChange(Property.X_END.name(), oldXEnd, xEnd);
    }
  }

  public float getYEnd() {
    return this.yEnd;
  }

  public void setYEnd(float yEnd) {
    if (yEnd != this.yEnd) {
      float oldYEnd = this.yEnd;
      this.yEnd = yEnd;
      this.shapeCache = null;
      this.propertyChangeSupport.firePropertyChange(Property.Y_END.name(), oldYEnd, yEnd);
    }
  }

  public float getOffset() {
    return this.offset;
  }

  public void setOffset(float offset) {
    if (offset != this.offset) {
      float oldOffset = this.offset;
      this.offset = offset;
      this.shapeCache = null;
      this.propertyChangeSupport.firePropertyChange(Property.Y_END.name(), oldOffset, offset);
    }
  }

  public float getLength() {
    return (float)Point2D.distance(getXStart(), getYStart(), getXEnd(), getYEnd());
  }

  public TextStyle getLengthStyle() {
    return this.lengthStyle;  
  }

  public void setLengthStyle(TextStyle lengthStyle) {
    if (lengthStyle != this.lengthStyle) {
      TextStyle oldLengthStyle = this.lengthStyle;
      this.lengthStyle = lengthStyle;
      this.propertyChangeSupport.firePropertyChange(Property.LENGTH_STYLE.name(), oldLengthStyle, lengthStyle);
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
    double angle = Math.atan2(this.yEnd - this.yStart, this.xEnd - this.xStart);
    float dx = (float)-Math.sin(angle) * this.offset;
    float dy = (float)Math.cos(angle) * this.offset;
    
    return new float [] [] {{this.xStart, this.yStart},
                            {this.xStart + dx, this.yStart + dy},
                            {this.xEnd + dx, this.yEnd + dy},
                            {this.xEnd, this.yEnd}};
  }

  public boolean intersectsRectangle(float x0, float y0, float x1, float y1) {
    Rectangle2D rectangle = new Rectangle2D.Float(x0, y0, 0, 0);
    rectangle.add(x1, y1);
    return getShape().intersects(rectangle);
  }

  public boolean containsPoint(float x, float y, float margin) {
    return containsShapeAtWithMargin(getShape(), x, y, margin);
  }

  public boolean isMiddlePointAt(float x, float y, float margin) {
    double angle = Math.atan2(this.yEnd - this.yStart, this.xEnd - this.xStart);
    float dx = (float)-Math.sin(angle) * this.offset;
    float dy = (float)Math.cos(angle) * this.offset;
    float xMiddle = (xStart + xEnd) / 2 + dx;
    float yMiddle = (yStart + yEnd) / 2 + dy;
    return Math.abs(x - xMiddle) <= margin && Math.abs(y - yMiddle) <= margin;
  }

  public boolean containsStartExtensionLinetAt(float x, float y, float margin) {
    double angle = Math.atan2(this.yEnd - this.yStart, this.xEnd - this.xStart);
    Line2D startExtensionLine = new Line2D.Float(this.xStart, this.yStart, 
        this.xStart + (float)-Math.sin(angle) * this.offset, 
        this.yStart + (float)Math.cos(angle) * this.offset);
    return containsShapeAtWithMargin(startExtensionLine, x, y, margin);
  }

  public boolean containsEndExtensionLineAt(float x, float y, float margin) {
    double angle = Math.atan2(this.yEnd - this.yStart, this.xEnd - this.xStart);
    Line2D endExtensionLine = new Line2D.Float(this.xEnd, this.yEnd, 
        this.xEnd + (float)-Math.sin(angle) * this.offset, 
        this.yEnd + (float)Math.cos(angle) * this.offset); 
    return containsShapeAtWithMargin(endExtensionLine, x, y, margin);
  }

  private boolean containsShapeAtWithMargin(Shape shape, float x, float y, float margin) {
    if (margin == 0) {
      return shape.contains(x, y);
    } else {
      return shape.intersects(x - margin, y - margin, 2 * margin, 2 * margin);
    }
  }

  private Shape getShape() {    
    if (this.shapeCache == null) {
      // 사각형 생성
      double angle = Math.atan2(this.yEnd - this.yStart, this.xEnd - this.xStart);
      float dx = (float)-Math.sin(angle) * this.offset;
      float dy = (float)Math.cos(angle) * this.offset;
      
      GeneralPath dimensionLineShape = new GeneralPath();
      // 넓이선 추가
      dimensionLineShape.append(new Line2D.Float(this.xStart + dx, this.yStart + dy, this.xEnd + dx, this.yEnd + dy), false);
      // 확장선 추가
      dimensionLineShape.append(new Line2D.Float(this.xStart, this.yStart, this.xStart + dx, this.yStart + dy), false);
      dimensionLineShape.append(new Line2D.Float(this.xEnd, this.yEnd, this.xEnd + dx, this.yEnd + dy), false);
      // 캐시 형성
      this.shapeCache = dimensionLineShape;
    }
    return this.shapeCache;
  }

  public void move(float dx, float dy) {
    setXStart(getXStart() + dx);
    setYStart(getYStart() + dy);
    setXEnd(getXEnd() + dx);
    setYEnd(getYEnd() + dy);
  }

  public DimensionLine clone() {
    DimensionLine clone = (DimensionLine)super.clone();
    clone.propertyChangeSupport = new PropertyChangeSupport(clone);
    clone.level = null;
    return clone;
  }
}
