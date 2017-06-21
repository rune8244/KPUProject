package com.eteks.homeview3d.model;

import java.awt.BasicStroke;
import java.awt.Shape;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Arrays;

public class Polyline extends HomeObject implements Selectable, Elevatable {
  private static final long serialVersionUID = 1L;

  public enum Property {POINTS, THICKNESS, CAP_STYLE, JOIN_STYLE, DASH_STYLE, START_ARROW_STYLE, END_ARROW_STYLE, CLOSED_PATH, COLOR, LEVEL}

  public enum CapStyle {BUTT, SQUARE, ROUND}
  
  public enum JoinStyle {BEVEL, MITER, ROUND, CURVED}
  
  public enum ArrowStyle {NONE, DELTA, OPEN, DISC}
  
  public enum DashStyle {SOLID, DOT, DASH, DASH_DOT, DASH_DOT_DOT}

  private float [][]           points;
  private float                thickness;
  private transient CapStyle   capStyle;
  private String               capStyleName;
  private transient JoinStyle  joinStyle;
  private String               joinStyleName;
  private transient DashStyle  dashStyle;
  private String               dashStyleName;
  private transient ArrowStyle startArrowStyle;
  private String               startArrowStyleName;
  private transient ArrowStyle endArrowStyle;
  private String               endArrowStyleName;
  private boolean              closedPath; 
  private int                  color;
  private Level                level;
  
  private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
  private transient Shape polylinePathCache;
  private transient Shape shapeCache;

  public Polyline(float [][] points) {
    this(points, 1, CapStyle.BUTT, JoinStyle.MITER, DashStyle.SOLID, ArrowStyle.NONE, ArrowStyle.NONE, false, 0xFF000000);
  }

  public Polyline(float [][] points, float thickness, 
                  CapStyle capStyle, JoinStyle joinStyle, DashStyle dashStyle, 
                  ArrowStyle startArrowStyle, ArrowStyle endArrowStyle,
                  boolean closedPath, int color) {
    this.points = deepCopy(points);
    this.thickness = thickness;
    this.capStyle = capStyle;
    this.joinStyle = joinStyle; 
    this.dashStyle = dashStyle;
    this.startArrowStyle = startArrowStyle;
    this.endArrowStyle = endArrowStyle;
    this.closedPath = closedPath;
    this.color = color;
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    in.defaultReadObject();
    try {
      if (this.capStyleName != null) {
        this.capStyle = CapStyle.valueOf(this.capStyleName);
        this.capStyleName = null;
      }
    } catch (IllegalArgumentException ex) {
    }
    try {
      if (this.joinStyleName != null) {
        this.joinStyle = JoinStyle.valueOf(this.joinStyleName);
        this.joinStyleName = null;
      }
    } catch (IllegalArgumentException ex) {
    }
    try {
      if (this.dashStyleName != null) {
        this.dashStyle = DashStyle.valueOf(this.dashStyleName);
        this.dashStyleName = null;
      }
    } catch (IllegalArgumentException ex) {
    }
    try {
      if (this.startArrowStyleName != null) {
        this.startArrowStyle = ArrowStyle.valueOf(this.startArrowStyleName);
        this.startArrowStyleName = null;
      }
    } catch (IllegalArgumentException ex) {
    }
    try {
      if (this.endArrowStyleName != null) {
        this.endArrowStyle = ArrowStyle.valueOf(this.endArrowStyleName);
        this.endArrowStyleName = null;
      }
    } catch (IllegalArgumentException ex) { 
    }
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    this.capStyleName = this.capStyle.name();
    this.joinStyleName = this.joinStyle.name();
    this.dashStyleName = this.dashStyle.name();
    this.startArrowStyleName = this.startArrowStyle.name();
    this.endArrowStyleName = this.endArrowStyle.name();
    out.defaultWriteObject();
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(listener);
  }

  public float [][] getPoints() {
    return deepCopy(this.points);  
  }

  public int getPointCount() {
    return this.points.length;  
  }

  private float [][] deepCopy(float [][] points) {
    float [][] pointsCopy = new float [points.length][];
    for (int i = 0; i < points.length; i++) {
      pointsCopy [i] = points [i].clone();
    }
    return pointsCopy;
  }

  public void setPoints(float [][] points) {
    if (!Arrays.deepEquals(this.points, points)) {
      updatePoints(points);
    }
  }

  private void updatePoints(float [][] points) {
    float [][] oldPoints = this.points;
    this.points = deepCopy(points);
    this.polylinePathCache = null;
    this.shapeCache = null;
    this.propertyChangeSupport.firePropertyChange(Property.POINTS.name(), oldPoints, points);
  }

  public void addPoint(float x, float y) {
    addPoint(x, y, this.points.length);
  }

  public void addPoint(float x, float y, int index) {
    if (index < 0 || index > this.points.length) {
      throw new IndexOutOfBoundsException("Invalid index " + index);
    }
    
    float [][] newPoints = new float [this.points.length + 1][];
    System.arraycopy(this.points, 0, newPoints, 0, index);
    newPoints [index] = new float [] {x, y};
    System.arraycopy(this.points, index, newPoints, index + 1, this.points.length - index);
    
    float [][] oldPoints = this.points;
    this.points = newPoints;
    this.polylinePathCache = null;
    this.shapeCache = null;
    this.propertyChangeSupport.firePropertyChange(Property.POINTS.name(), oldPoints, deepCopy(this.points));
  }

  public void setPoint(float x, float y, int index) {
    if (index < 0 || index >= this.points.length) {
      throw new IndexOutOfBoundsException("Invalid index " + index);
    }
    if (this.points [index][0] != x 
        || this.points [index][1] != y) {
      float [][] oldPoints = this.points;
      this.points = deepCopy(this.points);
      this.points [index][0] = x;
      this.points [index][1] = y;
      this.polylinePathCache = null;
      this.shapeCache = null;
      this.propertyChangeSupport.firePropertyChange(Property.POINTS.name(), oldPoints, deepCopy(this.points));
    }
  }

  public void removePoint(int index) {
    if (index < 0 || index >= this.points.length) {
      throw new IndexOutOfBoundsException("Invalid index " + index);
    }
    
    float [][] newPoints = new float [this.points.length - 1][];
    System.arraycopy(this.points, 0, newPoints, 0, index);
    System.arraycopy(this.points, index + 1, newPoints, index, this.points.length - index - 1);
    
    float [][] oldPoints = this.points;
    this.points = newPoints;
    this.polylinePathCache = null;
    this.shapeCache = null;
    this.propertyChangeSupport.firePropertyChange(Property.POINTS.name(), oldPoints, deepCopy(this.points));
  }

  public float getThickness() {
    return this.thickness;
  }

  public void setThickness(float thickness) {
    if (thickness != this.thickness) {
      float oldThickness = this.thickness;
      this.thickness = thickness;
      this.propertyChangeSupport.firePropertyChange(Property.THICKNESS.name(), oldThickness, thickness);
    }
  }

  public CapStyle getCapStyle() {
    return this.capStyle;
  }

  public void setCapStyle(CapStyle capStyle) {
    if (capStyle != this.capStyle) {
      CapStyle oldStyle = this.capStyle;
      this.capStyle = capStyle;
      this.propertyChangeSupport.firePropertyChange(Property.CAP_STYLE.name(), oldStyle, capStyle);
    }
  }

  public JoinStyle getJoinStyle() {
    return this.joinStyle;
  }

  public void setJoinStyle(JoinStyle joinStyle) {
    if (joinStyle != this.joinStyle) {
      JoinStyle oldJoinStyle = this.joinStyle;
      this.joinStyle = joinStyle;
      this.polylinePathCache = null;
      this.shapeCache = null;
      this.propertyChangeSupport.firePropertyChange(Property.JOIN_STYLE.name(), oldJoinStyle, joinStyle);
    }
  }

  public DashStyle getDashStyle() {
    return this.dashStyle;
  }

  public void setDashStyle(DashStyle dashStyle) {
    if (dashStyle != this.dashStyle) {
      DashStyle oldDashStyle = this.dashStyle;
      this.dashStyle = dashStyle;
      this.propertyChangeSupport.firePropertyChange(Property.DASH_STYLE.name(), oldDashStyle, dashStyle);
    }
  }

  public ArrowStyle getStartArrowStyle() {
    return this.startArrowStyle;
  }

  public void setStartArrowStyle(ArrowStyle startArrowStyle) {
    if (startArrowStyle != this.startArrowStyle) {
      ArrowStyle oldStartArrowStyle = this.startArrowStyle;
      this.startArrowStyle = startArrowStyle;
      this.propertyChangeSupport.firePropertyChange(Property.START_ARROW_STYLE.name(), oldStartArrowStyle, startArrowStyle);
    }
  }

  public ArrowStyle getEndArrowStyle() {
    return this.endArrowStyle;
  }

  public void setEndArrowStyle(ArrowStyle endArrowStyle) {
    if (endArrowStyle != this.endArrowStyle) {
      ArrowStyle oldEndArrowStyle = this.endArrowStyle;
      this.endArrowStyle = endArrowStyle;
      this.propertyChangeSupport.firePropertyChange(Property.END_ARROW_STYLE.name(), oldEndArrowStyle, endArrowStyle);
    }
  }

  public boolean isClosedPath() {
    return this.closedPath;
  }

  public void setClosedPath(boolean closedPath) {
    if (closedPath != this.closedPath) {
      this.closedPath = closedPath;
      this.propertyChangeSupport.firePropertyChange(Property.CLOSED_PATH.name(), !closedPath, closedPath);
    }
  }
  
  public int getColor() {
    return this.color;
  }

  public void setColor(int color) {
    if (color != this.color) {
      int oldColor = this.color;
      this.color = color;
      this.propertyChangeSupport.firePropertyChange(Property.COLOR.name(), oldColor, color);
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

  public float getLength() {
    float [] firstPoint = new float [2];
    float [] previousPoint = new float [2];
    float [] point = new float [2];
    float length = 0;
    for (PathIterator it = getPolylinePath().getPathIterator(null, 0.1); !it.isDone(); it.next()) {
      switch (it.currentSegment(point)) {
        case PathIterator.SEG_CLOSE :
          length += Point2D.distance(firstPoint [0], firstPoint [1], previousPoint [0], previousPoint [1]);
          break;
        case PathIterator.SEG_MOVETO :
          System.arraycopy(point, 0, firstPoint, 0, 2);
          System.arraycopy(point, 0, previousPoint, 0, 2);
          break;
        case PathIterator.SEG_LINETO :
          length += Point2D.distance(previousPoint [0], previousPoint [1], point [0], point [1]);
          System.arraycopy(point, 0, previousPoint, 0, 2);
          break;
      }
    }
    return length;
  }

  public boolean intersectsRectangle(float x0, float y0, float x1, float y1) {
    Rectangle2D rectangle = new Rectangle2D.Float(x0, y0, 0, 0);
    rectangle.add(x1, y1);
    return getShape().intersects(rectangle);
  }

  public boolean containsPoint(float x, float y, float margin) {
    return containsShapeAtWithMargin(getShape(), x, y, margin);
  }

  public int getPointIndexAt(float x, float y, float margin) {
    for (int i = 0; i < this.points.length; i++) {
      if (Math.abs(x - this.points [i][0]) <= margin && Math.abs(y - this.points [i][1]) <= margin) {
        return i;
      }
    }
    return -1;
  }

  private boolean containsShapeAtWithMargin(Shape shape, float x, float y, float margin) {
    if (margin == 0) {
      return shape.contains(x, y);
    } else {
      return shape.intersects(x - margin, y - margin, 2 * margin, 2 * margin);
    }
  }

  private Shape getPolylinePath() {
    if (this.polylinePathCache == null) {
      GeneralPath polylinePath = new GeneralPath();
      if (this.joinStyle == JoinStyle.CURVED) {
        for (int i = 0, n = this.closedPath ? this.points.length : this.points.length - 1; i < n; i++) {
          CubicCurve2D.Float curve2D = new CubicCurve2D.Float();
          float [] previousPoint = this.points [i == 0 ?  this.points.length - 1  : i - 1];
          float [] point         = this.points [i];
          float [] nextPoint     = this.points [i == this.points.length - 1 ?  0  : i + 1];
          float [] vectorToBisectorPoint = new float [] {nextPoint [0] - previousPoint [0], nextPoint [1] - previousPoint [1]};
          float [] nextNextPoint     = this.points [(i + 2) % this.points.length];
          float [] vectorToBisectorNextPoint = new float [] {point[0] - nextNextPoint [0], point[1] - nextNextPoint [1]};
          curve2D.setCurve(point[0], point[1], 
              point [0] + (i != 0 || this.closedPath  ? vectorToBisectorPoint [0] / 3.625f  : 0), 
              point [1] + (i != 0 || this.closedPath  ? vectorToBisectorPoint [1] / 3.625f  : 0), 
              nextPoint [0] + (i != this.points.length - 2 || this.closedPath  ? vectorToBisectorNextPoint [0] / 3.625f  : 0), 
              nextPoint [1] + (i != this.points.length - 2 || this.closedPath  ? vectorToBisectorNextPoint [1] / 3.625f  : 0), 
              nextPoint [0], nextPoint [1]);
          polylinePath.append(curve2D, true);
        }
      } else {
        polylinePath.moveTo(this.points [0][0], this.points [0][1]);
        for (int i = 1; i < this.points.length; i++) {
          polylinePath.lineTo(this.points [i][0], this.points [i][1]);
        }        
        if (this.closedPath) {
          polylinePath.closePath();
        }
      }
      this.polylinePathCache = polylinePath;
    }
    return this.polylinePathCache;
  }

  private Shape getShape() {
    if (this.shapeCache == null) {
      this.shapeCache = new BasicStroke(this.thickness).createStrokedShape(getPolylinePath());
    }
    return this.shapeCache;
  }

  public void move(float dx, float dy) {
    if (dx != 0 || dy != 0) {
      float [][] points = getPoints();
      for (int i = 0; i < points.length; i++) {
        points [i][0] += dx;
        points [i][1] += dy;
      }
      updatePoints(points);
    }
  }

  public Polyline clone() {
    Polyline clone = (Polyline)super.clone();
    clone.propertyChangeSupport = new PropertyChangeSupport(clone);
    clone.level = null;
    return clone;
  }
}
