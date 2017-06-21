package com.eteks.homeview3d.model;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Room extends HomeObject implements Selectable, Elevatable {
  public enum Property {NAME, NAME_X_OFFSET, NAME_Y_OFFSET, NAME_STYLE, NAME_ANGLE,
      POINTS, AREA_VISIBLE, AREA_X_OFFSET, AREA_Y_OFFSET, AREA_STYLE, AREA_ANGLE,
      FLOOR_COLOR, FLOOR_TEXTURE, FLOOR_VISIBLE, FLOOR_SHININESS,
      CEILING_COLOR, CEILING_TEXTURE, CEILING_VISIBLE, CEILING_SHININESS, LEVEL}
  
  private static final long serialVersionUID = 1L;
  
  private static final double TWICE_PI = 2 * Math.PI;

  private String              name;
  private float               nameXOffset;
  private float               nameYOffset;
  private TextStyle           nameStyle;
  private float               nameAngle;
  private float [][]          points;
  private boolean             areaVisible;
  private float               areaXOffset;
  private float               areaYOffset;
  private TextStyle           areaStyle;
  private float               areaAngle;
  private boolean             floorVisible;
  private Integer             floorColor;
  private HomeTexture         floorTexture;
  private float               floorShininess;
  private boolean             ceilingVisible;
  private Integer             ceilingColor;
  private HomeTexture         ceilingTexture;
  private float               ceilingShininess;
  private Level               level;
  
  private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
  private transient Shape shapeCache;
  private transient Float areaCache;

  /**
   * 이름과 지정된 좌표로부터 방 생성.
   */
  public Room(float [][] points) {
    if (points.length <= 1) {
      throw new IllegalStateException("Room points must containt at least two points");
    }
    this.points = deepCopy(points);
    this.areaVisible = true;
    this.nameYOffset = -40f;
    this.floorVisible = true;
    this.ceilingVisible = true;
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

  public float getNameXOffset() {
    return this.nameXOffset;  
  }

  public void setNameXOffset(float nameXOffset) {
    if (nameXOffset != this.nameXOffset) {
      float oldNameXOffset = this.nameXOffset;
      this.nameXOffset = nameXOffset;
      this.propertyChangeSupport.firePropertyChange(Property.NAME_X_OFFSET.name(), oldNameXOffset, nameXOffset);
    }
  }

  public float getNameYOffset() {
    return this.nameYOffset;  
  }

  public void setNameYOffset(float nameYOffset) {
    if (nameYOffset != this.nameYOffset) {
      float oldNameYOffset = this.nameYOffset;
      this.nameYOffset = nameYOffset;
      this.propertyChangeSupport.firePropertyChange(Property.NAME_Y_OFFSET.name(), oldNameYOffset, nameYOffset);
    }
  }

  public TextStyle getNameStyle() {
    return this.nameStyle;  
  }

  public void setNameStyle(TextStyle nameStyle) {
    if (nameStyle != this.nameStyle) {
      TextStyle oldNameStyle = this.nameStyle;
      this.nameStyle = nameStyle;
      this.propertyChangeSupport.firePropertyChange(Property.NAME_STYLE.name(), oldNameStyle, nameStyle);
    }
  }

  public float getNameAngle() {
    return this.nameAngle;
  }

  public void setNameAngle(float nameAngle) {
    nameAngle = (float)((nameAngle % TWICE_PI + TWICE_PI) % TWICE_PI);
    if (nameAngle != this.nameAngle) {
      float oldNameAngle = this.nameAngle;
      this.nameAngle = nameAngle;
      this.propertyChangeSupport.firePropertyChange(Property.NAME_ANGLE.name(), oldNameAngle, nameAngle);
    }
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
    this.shapeCache = null;
    this.areaCache  = null;
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
    this.shapeCache = null;
    this.areaCache  = null;
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
      this.shapeCache = null;
      this.areaCache  = null;
      this.propertyChangeSupport.firePropertyChange(Property.POINTS.name(), oldPoints, deepCopy(this.points));
    }
  }

  public void removePoint(int index) {
    if (index < 0 || index >= this.points.length) {
      throw new IndexOutOfBoundsException("Invalid index " + index);
    } else if (this.points.length <= 1) {
      throw new IllegalStateException("Room points must containt at least one point");
    }
    
    float [][] newPoints = new float [this.points.length - 1][];
    System.arraycopy(this.points, 0, newPoints, 0, index);
    System.arraycopy(this.points, index + 1, newPoints, index, this.points.length - index - 1);
    
    float [][] oldPoints = this.points;
    this.points = newPoints;
    this.shapeCache = null;
    this.areaCache  = null;
    this.propertyChangeSupport.firePropertyChange(Property.POINTS.name(), oldPoints, deepCopy(this.points));
  }

  public boolean isAreaVisible() {
    return this.areaVisible;  
  }

  public void setAreaVisible(boolean areaVisible) {
    if (areaVisible != this.areaVisible) {
      this.areaVisible = areaVisible;
      this.propertyChangeSupport.firePropertyChange(Property.AREA_VISIBLE.name(), !areaVisible, areaVisible);
    }
  }

  public float getAreaXOffset() {
    return this.areaXOffset;  
  }

  public void setAreaXOffset(float areaXOffset) {
    if (areaXOffset != this.areaXOffset) {
      float oldAreaXOffset = this.areaXOffset;
      this.areaXOffset = areaXOffset;
      this.propertyChangeSupport.firePropertyChange(Property.AREA_X_OFFSET.name(), oldAreaXOffset, areaXOffset);
    }
  }

  public float getAreaYOffset() {
    return this.areaYOffset;  
  }

  public void setAreaYOffset(float areaYOffset) {
    if (areaYOffset != this.areaYOffset) {
      float oldAreaYOffset = this.areaYOffset;
      this.areaYOffset = areaYOffset;
      this.propertyChangeSupport.firePropertyChange(Property.AREA_Y_OFFSET.name(), oldAreaYOffset, areaYOffset);
    }
  }

  public TextStyle getAreaStyle() {
    return this.areaStyle;  
  }

  public void setAreaStyle(TextStyle areaStyle) {
    if (areaStyle != this.areaStyle) {
      TextStyle oldAreaStyle = this.areaStyle;
      this.areaStyle = areaStyle;
      this.propertyChangeSupport.firePropertyChange(Property.AREA_STYLE.name(), oldAreaStyle, areaStyle);
    }
  }

  public float getAreaAngle() {
    return this.areaAngle;
  }

  public void setAreaAngle(float areaAngle) {
    areaAngle = (float)((areaAngle % TWICE_PI + TWICE_PI) % TWICE_PI);
    if (areaAngle != this.areaAngle) {
      float oldAreaAngle = this.areaAngle;
      this.areaAngle = areaAngle;
      this.propertyChangeSupport.firePropertyChange(Property.AREA_ANGLE.name(), oldAreaAngle, areaAngle);
    }
  }

  public float getXCenter() {
    float xMin = this.points [0][0]; 
    float xMax = this.points [0][0]; 
    for (int i = 1; i < this.points.length; i++) {
      xMin = Math.min(xMin, this.points [i][0]);
      xMax = Math.max(xMax, this.points [i][0]);
    }
    return (xMin + xMax) / 2;
  }
  
  public float getYCenter() {
    float yMin = this.points [0][1]; 
    float yMax = this.points [0][1]; 
    for (int i = 1; i < this.points.length; i++) {
      yMin = Math.min(yMin, this.points [i][1]);
      yMax = Math.max(yMax, this.points [i][1]);
    }
    return (yMin + yMax) / 2;
  }

  public Integer getFloorColor() {
    return this.floorColor;
  }

  public void setFloorColor(Integer floorColor) {
    if (floorColor != this.floorColor
        && (floorColor == null || !floorColor.equals(this.floorColor))) {
      Integer oldFloorColor = this.floorColor;
      this.floorColor = floorColor;
      this.propertyChangeSupport.firePropertyChange(Property.FLOOR_COLOR.name(), 
          oldFloorColor, floorColor);
    }
  }

  public HomeTexture getFloorTexture() {
    return this.floorTexture;
  }

  public void setFloorTexture(HomeTexture floorTexture) {
    if (floorTexture != this.floorTexture
        && (floorTexture == null || !floorTexture.equals(this.floorTexture))) {
      HomeTexture oldFloorTexture = this.floorTexture;
      this.floorTexture = floorTexture;
      this.propertyChangeSupport.firePropertyChange(Property.FLOOR_TEXTURE.name(), 
          oldFloorTexture, floorTexture);
    }
  }

  public boolean isFloorVisible() {
    return this.floorVisible;  
  }

  public void setFloorVisible(boolean floorVisible) {
    if (floorVisible != this.floorVisible) {
      this.floorVisible = floorVisible;
      this.propertyChangeSupport.firePropertyChange(Property.FLOOR_VISIBLE.name(), !floorVisible, floorVisible);
    }
  }

  public float getFloorShininess() {
    return this.floorShininess;
  }

  public void setFloorShininess(float floorShininess) {
    if (floorShininess != this.floorShininess) {
      float oldFloorShininess = this.floorShininess;
      this.floorShininess = floorShininess;
      this.propertyChangeSupport.firePropertyChange(Property.FLOOR_SHININESS.name(), 
          oldFloorShininess, floorShininess);
    }
  }

  public Integer getCeilingColor() {
    return this.ceilingColor;
  }

  public void setCeilingColor(Integer ceilingColor) {
    if (ceilingColor != this.ceilingColor
        && (ceilingColor == null || !ceilingColor.equals(this.ceilingColor))) {
      Integer oldCeilingColor = this.ceilingColor;
      this.ceilingColor = ceilingColor;
      this.propertyChangeSupport.firePropertyChange(Property.CEILING_COLOR.name(), 
          oldCeilingColor, ceilingColor);
    }
  }

  public HomeTexture getCeilingTexture() {
    return this.ceilingTexture;
  }

  public void setCeilingTexture(HomeTexture ceilingTexture) {
    if (ceilingTexture != this.ceilingTexture
        && (ceilingTexture == null || !ceilingTexture.equals(this.ceilingTexture))) {
      HomeTexture oldCeilingTexture = this.ceilingTexture;
      this.ceilingTexture = ceilingTexture;
      this.propertyChangeSupport.firePropertyChange(Property.CEILING_TEXTURE.name(), 
          oldCeilingTexture, ceilingTexture);
    }
  }

  public boolean isCeilingVisible() {
    return this.ceilingVisible;  
  }

  public void setCeilingVisible(boolean ceilingVisible) {
    if (ceilingVisible != this.ceilingVisible) {
      this.ceilingVisible = ceilingVisible;
      this.propertyChangeSupport.firePropertyChange(Property.CEILING_VISIBLE.name(), !ceilingVisible, ceilingVisible);
    }
  }

  public float getCeilingShininess() {
    return this.ceilingShininess;
  }

  public void setCeilingShininess(float ceilingShininess) {
    if (ceilingShininess != this.ceilingShininess) {
      float oldCeilingShininess = this.ceilingShininess;
      this.ceilingShininess = ceilingShininess;
      this.propertyChangeSupport.firePropertyChange(Property.CEILING_SHININESS.name(), 
          oldCeilingShininess, ceilingShininess);
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

  public float getArea() {
    if (this.areaCache == null) {
      Area roomArea = new Area(getShape());
      if (roomArea.isSingular()) {
        this.areaCache = Math.abs(getSignedArea(getPoints()));
      } else {
        float area = 0;
        List<float []> currentPathPoints = new ArrayList<float[]>();
        for (PathIterator it = roomArea.getPathIterator(null); !it.isDone(); ) {
          float [] roomPoint = new float[2];
          switch (it.currentSegment(roomPoint)) {
            case PathIterator.SEG_MOVETO : 
              currentPathPoints.add(roomPoint);
              break;
            case PathIterator.SEG_LINETO : 
              currentPathPoints.add(roomPoint);
              break;
            case PathIterator.SEG_CLOSE :
              float [][] pathPoints = 
                  currentPathPoints.toArray(new float [currentPathPoints.size()][]);
              area += getSignedArea(pathPoints);
              currentPathPoints.clear();
              break;
          }
          it.next();        
        }
        this.areaCache = area;
      }
    }
    return this.areaCache;
  }
  
  private float getSignedArea(float areaPoints [][]) {
    float area = 0;
    for (int i = 1; i < areaPoints.length; i++) {
      area += areaPoints [i][0] * areaPoints [i - 1][1];
      area -= areaPoints [i][1] * areaPoints [i - 1][0];
    }
    area += areaPoints [0][0] * areaPoints [areaPoints.length - 1][1];
    area -= areaPoints [0][1] * areaPoints [areaPoints.length - 1][0];
    return area / 2;
  }

  public boolean isClockwise() {
    return getSignedArea(getPoints()) < 0;
  }

  public boolean isSingular() {
    return new Area(getShape()).isSingular();
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

  public boolean isNameCenterPointAt(float x, float y, float margin) {
    return Math.abs(x - getXCenter() - getNameXOffset()) <= margin 
        && Math.abs(y - getYCenter() - getNameYOffset()) <= margin;
  }

  public boolean isAreaCenterPointAt(float x, float y, float margin) {
    return Math.abs(x - getXCenter() - getAreaXOffset()) <= margin 
        && Math.abs(y - getYCenter() - getAreaYOffset()) <= margin;
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
      GeneralPath roomShape = new GeneralPath();
      roomShape.moveTo(this.points [0][0], this.points [0][1]);
      for (int i = 1; i < this.points.length; i++) {
        roomShape.lineTo(this.points [i][0], this.points [i][1]);
      }
      roomShape.closePath();
      this.shapeCache = roomShape;
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

  public Room clone() {
    Room clone = (Room)super.clone();
    clone.propertyChangeSupport = new PropertyChangeSupport(clone);
    clone.level = null;
    return clone;
  }
}
