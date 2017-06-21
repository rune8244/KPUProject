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
import java.util.ArrayList;
import java.util.List;

public class Wall extends HomeObject implements Selectable, Elevatable {

  public enum Property {X_START, Y_START, X_END, Y_END, ARC_EXTENT, WALL_AT_START, WALL_AT_END, 
                        THICKNESS, HEIGHT, HEIGHT_AT_END, 
                        LEFT_SIDE_COLOR, LEFT_SIDE_TEXTURE, LEFT_SIDE_SHININESS, LEFT_SIDE_BASEBOARD, 
                        RIGHT_SIDE_COLOR, RIGHT_SIDE_TEXTURE, RIGHT_SIDE_SHININESS, RIGHT_SIDE_BASEBOARD,
                        PATTERN, TOP_COLOR, LEVEL}
  
  private static final long serialVersionUID = 1L;
  
  private float               xStart;
  private float               yStart;
  private float               xEnd;
  private float               yEnd; 
  private Float               arcExtent; 
  private Wall                wallAtStart;
  private Wall                wallAtEnd;
  private float               thickness;
  private Float               height;
  private Float               heightAtEnd;
  private Integer             leftSideColor;
  private HomeTexture         leftSideTexture;
  private float               leftSideShininess;
  private Baseboard           leftSideBaseboard;
  private Integer             rightSideColor;
  private HomeTexture         rightSideTexture;
  private float               rightSideShininess;
  private Baseboard           rightSideBaseboard;
  private boolean             symmetric = true;
  private TextureImage        pattern;  
  private Integer             topColor;
  private Level               level;
  
  private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
  private transient float [][] pointsCache;
  private transient float [][] pointsIncludingBaseboardsCache;

  public Wall(float xStart, float yStart, float xEnd, float yEnd, float thickness) {
    this(xStart, yStart, xEnd, yEnd, thickness, 0);
  }

  public Wall(float xStart, float yStart, float xEnd, float yEnd, float thickness, float height) {
    this(xStart, yStart, xEnd, yEnd, thickness, height, null);
  }

  public Wall(float xStart, float yStart, float xEnd, float yEnd, float thickness, float height, TextureImage pattern) {
    this.xStart = xStart;
    this.yStart = yStart;
    this.xEnd = xEnd;
    this.yEnd = yEnd;
    this.thickness = thickness;
    this.height = height;
    this.pattern = pattern;
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
      clearPointsCache();
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
      clearPointsCache();
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
      clearPointsCache();
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
      clearPointsCache();
      this.propertyChangeSupport.firePropertyChange(Property.Y_END.name(), oldYEnd, yEnd);
    }
  }

  public float getLength() {
    if (this.arcExtent == null
        || this.arcExtent.floatValue() == 0) {
      return (float)Point2D.distance(this.xStart, this.yStart, this.xEnd, this.yEnd);
    } else {
      float [] arcCircleCenter = getArcCircleCenter();
      float arcCircleRadius = (float)Point2D.distance(this.xStart, this.yStart, 
          arcCircleCenter [0], arcCircleCenter [1]);
      return Math.abs(this.arcExtent) * arcCircleRadius;
    }
  }

  public float getStartPointToEndPointDistance() {
    return (float)Point2D.distance(this.xStart, this.yStart, this.xEnd, this.yEnd);
  }

  public void setArcExtent(Float arcExtent) {
    if (arcExtent != this.arcExtent
        || (arcExtent != null && !arcExtent.equals(this.arcExtent))) {
      Float oldArcExtent = this.arcExtent;
      this.arcExtent = arcExtent;
      clearPointsCache();
      this.propertyChangeSupport.firePropertyChange(Property.ARC_EXTENT.name(), 
          oldArcExtent, arcExtent);
    }
  }

  public Float getArcExtent() {
    return this.arcExtent;
  }

  public float getXArcCircleCenter() {
    if (this.arcExtent == null) {
      return (this.xStart + this.xEnd) / 2; 
    } else {
      return getArcCircleCenter() [0];
    }
  }

  public float getYArcCircleCenter() {
    if (this.arcExtent == null) {
      return (this.yStart + this.yEnd) / 2;
    } else {
      return getArcCircleCenter() [1];
    }
  }

  private float [] getArcCircleCenter() {
    double startToEndPointsDistance = Point2D.distance(this.xStart, this.yStart, this.xEnd, this.yEnd);
    double wallToStartPointArcCircleCenterAngle = Math.abs(this.arcExtent) > Math.PI 
        ? -(Math.PI + this.arcExtent) / 2
        : (Math.PI - this.arcExtent) / 2;
    float arcCircleCenterToWallDistance = -(float)(Math.tan(wallToStartPointArcCircleCenterAngle) 
        * startToEndPointsDistance / 2); 
    float xMiddlePoint = (this.xStart + this.xEnd) / 2;
    float yMiddlePoint = (this.yStart + this.yEnd) / 2;
    double angle = Math.atan2(this.xStart - this.xEnd, this.yEnd - this.yStart);
    return new float [] {(float)(xMiddlePoint + arcCircleCenterToWallDistance * Math.cos(angle)), 
                         (float)(yMiddlePoint + arcCircleCenterToWallDistance * Math.sin(angle))};
  }
  public Wall getWallAtStart() {
    return this.wallAtStart;
  }

  public void setWallAtStart(Wall wallAtStart) {
    setWallAtStart(wallAtStart, true);
  }

  private void setWallAtStart(Wall wallAtStart, boolean detachJoinedWallAtStart) {
    if (wallAtStart != this.wallAtStart) {
      Wall oldWallAtStart = this.wallAtStart;
      this.wallAtStart = wallAtStart;
      clearPointsCache();
      this.propertyChangeSupport.firePropertyChange(Property.WALL_AT_START.name(), 
          oldWallAtStart, wallAtStart);
      
      if (detachJoinedWallAtStart) {
        detachJoinedWall(oldWallAtStart);
      }
    }
  }

  public Wall getWallAtEnd() {
    return this.wallAtEnd;
  }

  public void setWallAtEnd(Wall wallAtEnd) {
    setWallAtEnd(wallAtEnd, true);
  }

  private void setWallAtEnd(Wall wallAtEnd, boolean detachJoinedWallAtEnd) {
    if (wallAtEnd != this.wallAtEnd) {
      Wall oldWallAtEnd = this.wallAtEnd;
      this.wallAtEnd = wallAtEnd;
      clearPointsCache();
      this.propertyChangeSupport.firePropertyChange(Property.WALL_AT_END.name(), 
          oldWallAtEnd, wallAtEnd);
      
      if (detachJoinedWallAtEnd) {
        detachJoinedWall(oldWallAtEnd);
      }
    }
  }

  private void detachJoinedWall(Wall joinedWall) {
    if (joinedWall != null) {
      if (joinedWall.getWallAtStart() == this) {
        joinedWall.setWallAtStart(null, false);
      } else if (joinedWall.getWallAtEnd() == this) {
        joinedWall.setWallAtEnd(null, false);
      } 
    }
  }

  public float getThickness() {
    return this.thickness;
  }

  public void setThickness(float thickness) {
    if (thickness != this.thickness) {
      float oldThickness = this.thickness;
      this.thickness = thickness;
      clearPointsCache();
      this.propertyChangeSupport.firePropertyChange(Property.THICKNESS.name(), 
          oldThickness, thickness);
    }
  }

  public Float getHeight() {
    return this.height;
  }

  public void setHeight(Float height) {
    if (height != this.height
        || (height != null && !height.equals(this.height))) {
      Float oldHeight = this.height;
      this.height = height;
      this.propertyChangeSupport.firePropertyChange(Property.HEIGHT.name(), 
          oldHeight, height);
    }
  }

  public Float getHeightAtEnd() {
    return this.heightAtEnd;
  }

  public void setHeightAtEnd(Float heightAtEnd) {
    if (heightAtEnd != this.heightAtEnd
        && (heightAtEnd == null || !heightAtEnd.equals(this.heightAtEnd))) {
      Float oldHeightAtEnd = this.heightAtEnd;
      this.heightAtEnd = heightAtEnd;
      this.propertyChangeSupport.firePropertyChange(Property.HEIGHT_AT_END.name(), 
          oldHeightAtEnd, heightAtEnd);
    }
  }

  public boolean isTrapezoidal() {
    return this.height != null
        && this.heightAtEnd != null
        && !this.height.equals(this.heightAtEnd);  
  }

  public Integer getLeftSideColor() {
    return this.leftSideColor;
  }

  public void setLeftSideColor(Integer leftSideColor) {
    if (leftSideColor != this.leftSideColor
        && (leftSideColor == null || !leftSideColor.equals(this.leftSideColor))) {
      Integer oldLeftSideColor = this.leftSideColor;
      this.leftSideColor = leftSideColor;
      this.propertyChangeSupport.firePropertyChange(Property.LEFT_SIDE_COLOR.name(), 
          oldLeftSideColor, leftSideColor);
    }
  }

  public Integer getRightSideColor() {
    return this.rightSideColor;
  }

  public void setRightSideColor(Integer rightSideColor) {
    if (rightSideColor != this.rightSideColor
        && (rightSideColor == null || !rightSideColor.equals(this.rightSideColor))) {
      Integer oldLeftSideColor = this.rightSideColor;
      this.rightSideColor = rightSideColor;
      this.propertyChangeSupport.firePropertyChange(Property.RIGHT_SIDE_COLOR.name(), 
          oldLeftSideColor, rightSideColor);
    }
  }

  public HomeTexture getLeftSideTexture() {
    return this.leftSideTexture;
  }

  public void setLeftSideTexture(HomeTexture leftSideTexture) {
    if (leftSideTexture != this.leftSideTexture
        && (leftSideTexture == null || !leftSideTexture.equals(this.leftSideTexture))) {
      HomeTexture oldLeftSideTexture = this.leftSideTexture;
      this.leftSideTexture = leftSideTexture;
      this.propertyChangeSupport.firePropertyChange(Property.LEFT_SIDE_TEXTURE.name(), 
          oldLeftSideTexture, leftSideTexture);
    }
  }

  public HomeTexture getRightSideTexture() {
    return this.rightSideTexture;
  }

  public void setRightSideTexture(HomeTexture rightSideTexture) {
    if (rightSideTexture != this.rightSideTexture
        && (rightSideTexture == null || !rightSideTexture.equals(this.rightSideTexture))) {
      HomeTexture oldLeftSideTexture = this.rightSideTexture;
      this.rightSideTexture = rightSideTexture;
      this.propertyChangeSupport.firePropertyChange(Property.RIGHT_SIDE_TEXTURE.name(), 
          oldLeftSideTexture, rightSideTexture);
    }
  }

  public float getLeftSideShininess() {
    return this.leftSideShininess;
  }

  public void setLeftSideShininess(float leftSideShininess) {
    if (leftSideShininess != this.leftSideShininess) {
      float oldLeftSideShininess = this.leftSideShininess;
      this.leftSideShininess = leftSideShininess;
      this.propertyChangeSupport.firePropertyChange(Property.LEFT_SIDE_SHININESS.name(), oldLeftSideShininess, leftSideShininess);
    }
  }

  public float getRightSideShininess() {
    return this.rightSideShininess;
  }

  public void setRightSideShininess(float rightSideShininess) {
    if (rightSideShininess != this.rightSideShininess) {
      float oldRightSideShininess = this.rightSideShininess;
      this.rightSideShininess = rightSideShininess;
      this.propertyChangeSupport.firePropertyChange(Property.RIGHT_SIDE_SHININESS.name(), oldRightSideShininess, rightSideShininess);
    }
  }

  public Baseboard getLeftSideBaseboard() {
    return this.leftSideBaseboard;
  }

  public void setLeftSideBaseboard(Baseboard leftSideBaseboard) {
    if (leftSideBaseboard != this.leftSideBaseboard
        && (leftSideBaseboard == null || !leftSideBaseboard.equals(this.leftSideBaseboard))) {
      Baseboard oldLeftSideBaseboard = this.leftSideBaseboard;
      this.leftSideBaseboard = leftSideBaseboard;
      clearPointsCache();
      this.propertyChangeSupport.firePropertyChange(Property.LEFT_SIDE_BASEBOARD.name(), oldLeftSideBaseboard, leftSideBaseboard);
    }
  }

  public Baseboard getRightSideBaseboard() {
    return this.rightSideBaseboard;
  }

  public void setRightSideBaseboard(Baseboard rightSideBaseboard) {
    if (rightSideBaseboard != this.rightSideBaseboard
        && (rightSideBaseboard == null || !rightSideBaseboard.equals(this.rightSideBaseboard))) {
      Baseboard oldRightSideBaseboard = this.rightSideBaseboard;
      this.rightSideBaseboard = rightSideBaseboard;
      clearPointsCache();
      this.propertyChangeSupport.firePropertyChange(Property.RIGHT_SIDE_BASEBOARD.name(), oldRightSideBaseboard, rightSideBaseboard);
    }
  }

  public TextureImage getPattern() {
    return this.pattern;
  }

  public void setPattern(TextureImage pattern) {
    if (this.pattern != pattern) {
      TextureImage oldPattern = this.pattern;
      this.pattern = pattern;
      this.propertyChangeSupport.firePropertyChange(Property.PATTERN.name(), 
          oldPattern, pattern);
    }
  }

  public Integer getTopColor() {
    return this.topColor;
  }

  public void setTopColor(Integer topColor) {
    if (this.topColor != topColor
        && (topColor == null || !topColor.equals(this.topColor))) {
      Integer oldTopColor = this.topColor;
      this.topColor = topColor;
      this.propertyChangeSupport.firePropertyChange(Property.TOP_COLOR.name(), 
          oldTopColor, topColor);
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
    if (this.level == level) {
      return true;
    } else if (this.level != null && level != null) {
      float wallLevelElevation = this.level.getElevation();
      float levelElevation = level.getElevation();
      return wallLevelElevation == levelElevation
             && this.level.getElevationIndex() < level.getElevationIndex()
          || wallLevelElevation < levelElevation
             && wallLevelElevation + getWallMaximumHeight() > levelElevation;
    } else {
      return false;
    }
  }

  private float getWallMaximumHeight() {
    if (this.height == null) {
      return 0; 
    } else if (isTrapezoidal()) {
      return Math.max(this.height, this.heightAtEnd);
    } else {
      return this.height;
    }
  }

  private void clearPointsCache() {
    this.pointsCache = null;
    this.pointsIncludingBaseboardsCache = null;
    if (this.wallAtStart != null ) {
      this.wallAtStart.pointsCache = null;
      this.wallAtStart.pointsIncludingBaseboardsCache = null;
    }
    if (this.wallAtEnd != null) {
      this.wallAtEnd.pointsCache = null;
      this.wallAtEnd.pointsIncludingBaseboardsCache = null;
    }
  }

  public float [][] getPoints() {
    return getPoints(false);
  }

  public float [][] getPoints(boolean includeBaseboards) {
    if (includeBaseboards
        && (this.leftSideBaseboard != null
            || this.rightSideBaseboard != null)) {
      if (this.pointsIncludingBaseboardsCache == null) {
        this.pointsIncludingBaseboardsCache = getShapePoints(true);
      }
      return clonePoints(this.pointsIncludingBaseboardsCache);
    } else {
      if (this.pointsCache == null) {
        this.pointsCache = getShapePoints(false);
      }
      return clonePoints(this.pointsCache);
    }
  }

  private float [][] clonePoints(float [][] points) {
    float [][] clonedPoints = new float [points.length][];
    for (int i = 0; i < points.length; i++) {
      clonedPoints [i] = points [i].clone();
    }
    return clonedPoints;
  }

  private float [][] getShapePoints(boolean includeBaseboards) {
    final float epsilon = 0.01f;
    float [][] wallPoints = getUnjoinedShapePoints(includeBaseboards);
    int leftSideStartPointIndex = 0;
    int rightSideStartPointIndex = wallPoints.length - 1;
    int leftSideEndPointIndex = wallPoints.length / 2 - 1;
    int rightSideEndPointIndex = wallPoints.length / 2;
    float limit = 2 * this.thickness;
    if (this.wallAtStart != null) {
      float [][] wallAtStartPoints = this.wallAtStart.getUnjoinedShapePoints(includeBaseboards);
      int wallAtStartLeftSideStartPointIndex = 0;
      int wallAtStartRightSideStartPointIndex = wallAtStartPoints.length - 1;
      int wallAtStartLeftSideEndPointIndex = wallAtStartPoints.length / 2 - 1;
      int wallAtStartRightSideEndPointIndex = wallAtStartPoints.length / 2;
      boolean wallAtStartJoinedAtEnd = this.wallAtStart.getWallAtEnd() == this
          && (this.wallAtStart.getWallAtStart() != this
              || (this.wallAtStart.xEnd == this.xStart
                  && this.wallAtStart.yEnd == this.yStart));
      boolean wallAtStartJoinedAtStart = this.wallAtStart.getWallAtStart() == this 
          && (this.wallAtStart.getWallAtEnd() != this
              || (this.wallAtStart.xStart == this.xStart
                  && this.wallAtStart.yStart == this.yStart));
      float [][] wallAtStartPointsCache = includeBaseboards 
          ? this.wallAtStart.pointsIncludingBaseboardsCache
          : this.wallAtStart.pointsCache;
      if (wallAtStartJoinedAtEnd) {
        computeIntersection(wallPoints [leftSideStartPointIndex], wallPoints [leftSideStartPointIndex + 1], 
            wallAtStartPoints [wallAtStartLeftSideEndPointIndex], wallAtStartPoints [wallAtStartLeftSideEndPointIndex - 1], limit);
        computeIntersection(wallPoints [rightSideStartPointIndex], wallPoints [rightSideStartPointIndex - 1],  
            wallAtStartPoints [wallAtStartRightSideEndPointIndex], wallAtStartPoints [wallAtStartRightSideEndPointIndex + 1], limit);

        if (wallAtStartPointsCache != null) {
          if (Math.abs(wallPoints [leftSideStartPointIndex][0] - wallAtStartPointsCache [wallAtStartLeftSideEndPointIndex][0]) < epsilon
              && Math.abs(wallPoints [leftSideStartPointIndex][1] - wallAtStartPointsCache [wallAtStartLeftSideEndPointIndex][1]) < epsilon) {
            wallPoints [leftSideStartPointIndex] = wallAtStartPointsCache [wallAtStartLeftSideEndPointIndex];
          }                        
          if (Math.abs(wallPoints [rightSideStartPointIndex][0] - wallAtStartPointsCache [wallAtStartRightSideEndPointIndex][0]) < epsilon
              && Math.abs(wallPoints [rightSideStartPointIndex][1] - wallAtStartPointsCache [wallAtStartRightSideEndPointIndex][1]) < epsilon) {
            wallPoints [rightSideStartPointIndex] = wallAtStartPointsCache [wallAtStartRightSideEndPointIndex];
          }
        }
      } else if (wallAtStartJoinedAtStart) {
        computeIntersection(wallPoints [leftSideStartPointIndex], wallPoints [leftSideStartPointIndex + 1], 
            wallAtStartPoints [wallAtStartRightSideStartPointIndex], wallAtStartPoints [wallAtStartRightSideStartPointIndex - 1], limit);
        computeIntersection(wallPoints [rightSideStartPointIndex], wallPoints [rightSideStartPointIndex - 1],  
            wallAtStartPoints [wallAtStartLeftSideStartPointIndex], wallAtStartPoints [wallAtStartLeftSideStartPointIndex + 1], limit);
        
        if (wallAtStartPointsCache != null) {
          if (Math.abs(wallPoints [leftSideStartPointIndex][0] - wallAtStartPointsCache [wallAtStartRightSideStartPointIndex][0]) < epsilon
              && Math.abs(wallPoints [leftSideStartPointIndex][1] - wallAtStartPointsCache [wallAtStartRightSideStartPointIndex][1]) < epsilon) {
            wallPoints [leftSideStartPointIndex] = wallAtStartPointsCache [wallAtStartRightSideStartPointIndex];
          }                            
          if (wallAtStartPointsCache != null
              && Math.abs(wallPoints [rightSideStartPointIndex][0] - wallAtStartPointsCache [wallAtStartLeftSideStartPointIndex][0]) < epsilon
              && Math.abs(wallPoints [rightSideStartPointIndex][1] - wallAtStartPointsCache [wallAtStartLeftSideStartPointIndex][1]) < epsilon) {
            wallPoints [rightSideStartPointIndex] = wallAtStartPointsCache [wallAtStartLeftSideStartPointIndex];
          }
        }
      }
    }

    if (this.wallAtEnd != null) {
      float [][] wallAtEndPoints = this.wallAtEnd.getUnjoinedShapePoints(includeBaseboards);
      int wallAtEndLeftSideStartPointIndex = 0;
      int wallAtEndRightSideStartPointIndex = wallAtEndPoints.length - 1;
      int wallAtEndLeftSideEndPointIndex = wallAtEndPoints.length / 2 - 1;
      int wallAtEndRightSideEndPointIndex = wallAtEndPoints.length / 2;
      boolean wallAtEndJoinedAtStart = this.wallAtEnd.getWallAtStart() == this
          && (this.wallAtEnd.getWallAtEnd() != this
              || (this.wallAtEnd.xStart == this.xEnd
                  && this.wallAtEnd.yStart == this.yEnd));
      boolean wallAtEndJoinedAtEnd = this.wallAtEnd.getWallAtEnd() == this
          && (this.wallAtEnd.getWallAtStart() != this
              || (this.wallAtEnd.xEnd == this.xEnd
                  && this.wallAtEnd.yEnd == this.yEnd));
      float [][] wallAtEndPointsCache = includeBaseboards 
          ? this.wallAtEnd.pointsIncludingBaseboardsCache
          : this.wallAtEnd.pointsCache;
      if (wallAtEndJoinedAtStart) {
        computeIntersection(wallPoints [leftSideEndPointIndex], wallPoints [leftSideEndPointIndex - 1], 
            wallAtEndPoints [wallAtEndLeftSideStartPointIndex], wallAtEndPoints [wallAtEndLeftSideStartPointIndex + 1], limit);
        computeIntersection(wallPoints [rightSideEndPointIndex], wallPoints [rightSideEndPointIndex + 1], 
            wallAtEndPoints [wallAtEndRightSideStartPointIndex], wallAtEndPoints [wallAtEndRightSideStartPointIndex - 1], limit);
        if (wallAtEndPointsCache != null) {
          if (Math.abs(wallPoints [leftSideEndPointIndex][0] - wallAtEndPointsCache [wallAtEndLeftSideStartPointIndex][0]) < epsilon
              && Math.abs(wallPoints [leftSideEndPointIndex][1] - wallAtEndPointsCache [wallAtEndLeftSideStartPointIndex][1]) < epsilon) {
            wallPoints [leftSideEndPointIndex] = wallAtEndPointsCache [wallAtEndLeftSideStartPointIndex];
          }                        
          if (Math.abs(wallPoints [rightSideEndPointIndex][0] - wallAtEndPointsCache [wallAtEndRightSideStartPointIndex][0]) < epsilon
              && Math.abs(wallPoints [rightSideEndPointIndex][1] - wallAtEndPointsCache [wallAtEndRightSideStartPointIndex][1]) < epsilon) {
            wallPoints [rightSideEndPointIndex] = wallAtEndPointsCache [wallAtEndRightSideStartPointIndex];
          }
        }
      } else if (wallAtEndJoinedAtEnd) {
        computeIntersection(wallPoints [leftSideEndPointIndex], wallPoints [leftSideEndPointIndex - 1],  
            wallAtEndPoints [wallAtEndRightSideEndPointIndex], wallAtEndPoints [wallAtEndRightSideEndPointIndex + 1], limit);
        computeIntersection(wallPoints [rightSideEndPointIndex], wallPoints [rightSideEndPointIndex + 1], 
            wallAtEndPoints [wallAtEndLeftSideEndPointIndex], wallAtEndPoints [wallAtEndLeftSideEndPointIndex - 1], limit);

        if (wallAtEndPointsCache != null) {
          if (Math.abs(wallPoints [leftSideEndPointIndex][0] - wallAtEndPointsCache [wallAtEndRightSideEndPointIndex][0]) < epsilon
              && Math.abs(wallPoints [leftSideEndPointIndex][1] - wallAtEndPointsCache [wallAtEndRightSideEndPointIndex][1]) < epsilon) {
            wallPoints [leftSideEndPointIndex] = wallAtEndPointsCache [wallAtEndRightSideEndPointIndex];
          }                        
          if (Math.abs(wallPoints [rightSideEndPointIndex][0] - wallAtEndPointsCache [wallAtEndLeftSideEndPointIndex][0]) < epsilon
              && Math.abs(wallPoints [rightSideEndPointIndex][1] - wallAtEndPointsCache [wallAtEndLeftSideEndPointIndex][1]) < epsilon) {
            wallPoints [rightSideEndPointIndex] = wallAtEndPointsCache [wallAtEndLeftSideEndPointIndex];
          }
        }
      }
    }
    return wallPoints;
  }

  private float [][] getUnjoinedShapePoints(boolean includeBaseboards) {
    if (this.arcExtent != null
        && this.arcExtent.floatValue() != 0
        && Point2D.distanceSq(this.xStart, this.yStart, this.xEnd, this.yEnd) > 1E-10) {
      float [] arcCircleCenter = getArcCircleCenter();
      float startAngle = (float)Math.atan2(arcCircleCenter [1] - this.yStart, arcCircleCenter [0] - this.xStart);
      startAngle += 2 * (float)Math.atan2(this.yStart - this.yEnd, this.xEnd - this.xStart);
      float arcCircleRadius = (float)Point2D.distance(arcCircleCenter [0], arcCircleCenter [1], this.xStart, this.yStart);
      float exteriorArcRadius = arcCircleRadius + this.thickness / 2;
      float interiorArcRadius = Math.max(0, arcCircleRadius - this.thickness / 2);
      float exteriorArcLength = exteriorArcRadius * Math.abs(this.arcExtent);
      float angleDelta = this.arcExtent / (float)Math.sqrt(exteriorArcLength);
      int angleStepCount = (int)(this.arcExtent / angleDelta);
      if (includeBaseboards) {
        if (angleDelta > 0) {
          if (this.leftSideBaseboard != null) {
            exteriorArcRadius += this.leftSideBaseboard.getThickness();
          }
          if (this.rightSideBaseboard != null) {
            interiorArcRadius -= this.rightSideBaseboard.getThickness();
          }
        } else {
          if (this.leftSideBaseboard != null) {
            interiorArcRadius -= this.leftSideBaseboard.getThickness();
          }
          if (this.rightSideBaseboard != null) {
            exteriorArcRadius += this.rightSideBaseboard.getThickness();
          }
        }
      }
      List<float[]> wallPoints = new ArrayList<float[]>((angleStepCount + 2) * 2);      
      if (this.symmetric) {
        if (Math.abs(this.arcExtent - angleStepCount * angleDelta) > 1E-6) {
          angleDelta = this.arcExtent / ++angleStepCount;
        }
        for (int i = 0; i <= angleStepCount; i++) {
          computeRoundWallShapePoint(wallPoints, startAngle + this.arcExtent - i * angleDelta, i, angleDelta, 
              arcCircleCenter, exteriorArcRadius, interiorArcRadius);
        }
      } else {
        int i = 0;
        for (float angle = this.arcExtent; angleDelta > 0 ? angle >= angleDelta * 0.1f : angle <= -angleDelta * 0.1f; angle -= angleDelta, i++) {
          computeRoundWallShapePoint(wallPoints, startAngle + angle, i, angleDelta, 
              arcCircleCenter, exteriorArcRadius, interiorArcRadius);
        }
        computeRoundWallShapePoint(wallPoints, startAngle, i, angleDelta, 
            arcCircleCenter, exteriorArcRadius, interiorArcRadius);
      }
      return wallPoints.toArray(new float [wallPoints.size()][]);
    } else { 
      double angle = Math.atan2(this.yEnd - this.yStart, 
                                this.xEnd - this.xStart);
      float sin = (float)Math.sin(angle);
      float cos = (float)Math.cos(angle);
      float leftSideTickness = this.thickness / 2;
      if (includeBaseboards && this.leftSideBaseboard != null) {
        leftSideTickness += this.leftSideBaseboard.getThickness();
      }
      float leftSideDx = sin * leftSideTickness;
      float leftSideDy = cos * leftSideTickness;
      float rightSideTickness = this.thickness / 2;
      if (includeBaseboards && this.rightSideBaseboard != null) {
        rightSideTickness += this.rightSideBaseboard.getThickness();
      }
      float rightSideDx = sin * rightSideTickness;
      float rightSideDy = cos * rightSideTickness;
      return new float [][] {
          {this.xStart + leftSideDx, this.yStart - leftSideDy},
          {this.xEnd   + leftSideDx, this.yEnd   - leftSideDy},
          {this.xEnd   - rightSideDx, this.yEnd   + rightSideDy},
          {this.xStart - rightSideDx, this.yStart + rightSideDy}};
    }
  }

  private void computeRoundWallShapePoint(List<float []> wallPoints, float angle, int index, float angleDelta, 
                                          float [] arcCircleCenter, float exteriorArcRadius, float interiorArcRadius) {
    double cos = Math.cos(angle);
    double sin = Math.sin(angle);
    float [] interiorArcPoint = new float [] {(float)(arcCircleCenter [0] + interiorArcRadius * cos), 
                                              (float)(arcCircleCenter [1] - interiorArcRadius * sin)};
    float [] exteriorArcPoint = new float [] {(float)(arcCircleCenter [0] + exteriorArcRadius * cos), 
                                              (float)(arcCircleCenter [1] - exteriorArcRadius * sin)};
    if (angleDelta > 0) {
      wallPoints.add(index, interiorArcPoint);
      wallPoints.add(wallPoints.size() - 1 - index, exteriorArcPoint);
    } else {
      wallPoints.add(index, exteriorArcPoint);
      wallPoints.add(wallPoints.size() - 1 - index, interiorArcPoint);
    }
  }
  
  private void computeIntersection(float [] point1, float [] point2, 
                                   float [] point3, float [] point4, float limit) {
    float alpha1 = (point2 [1] - point1 [1]) / (point2 [0] - point1 [0]);
    float alpha2 = (point4 [1] - point3 [1]) / (point4 [0] - point3 [0]);
    // 2개의 선이 평행하지 않은 경우
    if (alpha1 != alpha2) {
      float x = point1 [0];
      float y = point1 [1];
      
      // 첫번째 줄이 수직일 경우
      if (Math.abs(alpha1) > 4000)  {
        if (Math.abs(alpha2) < 4000) {
          x = point1 [0];
          float beta2  = point4 [1] - alpha2 * point4 [0];
          y = alpha2 * x + beta2;
        }
      // 두번째 줄이 수직일 경우
      } else if (Math.abs(alpha2) > 4000) {
        if (Math.abs(alpha1) < 4000) {
          x = point3 [0];
          float beta1  = point2 [1] - alpha1 * point2 [0];
          y = alpha1 * x + beta1;
        }
      } else {
        boolean sameSignum = Math.signum(alpha1) == Math.signum(alpha2);
        if (Math.abs(alpha1 - alpha2) > 1E-5
            && (!sameSignum || (Math.abs(alpha1) > Math.abs(alpha2)   ? alpha1 / alpha2   : alpha2 / alpha1) > 1.004)) {
          float beta1 = point2 [1] - alpha1 * point2 [0];
          float beta2 = point4 [1] - alpha2 * point4 [0];
          x = (beta2 - beta1) / (alpha1 - alpha2);
          y = alpha1 * x + beta1;
        } 
      }
      
      if (Point2D.distanceSq(x, y, point1 [0], point1 [1]) < limit * limit) {
        point1 [0] = x;
        point1 [1] = y;
      }
    }
  }

  public boolean intersectsRectangle(float x0, float y0, float x1, float y1) {
    Rectangle2D rectangle = new Rectangle2D.Float(x0, y0, 0, 0);
    rectangle.add(x1, y1);
    return getShape(false).intersects(rectangle);
  }

  public boolean containsPoint(float x, float y, float margin) {
    return containsPoint(x, y, false, margin);
  }
  
  public boolean containsPoint(float x, float y, boolean includeBaseboards, float margin) {
    return containsShapeAtWithMargin(getShape(includeBaseboards), x, y, margin);
  }

  public boolean containsWallStartAt(float x, float y, float margin) {
    float [][] wallPoints = getPoints();
    Line2D startLine = new Line2D.Float(wallPoints [0][0], wallPoints [0][1], 
        wallPoints [wallPoints.length - 1][0], wallPoints [wallPoints.length - 1][1]);
    return containsShapeAtWithMargin(startLine, x, y, margin);
  }
  
  public boolean containsWallEndAt(float x, float y, float margin) {
    float [][] wallPoints = getPoints();
    Line2D endLine = new Line2D.Float(wallPoints [wallPoints.length / 2 - 1][0], wallPoints [wallPoints.length / 2 - 1][1], 
        wallPoints [wallPoints.length / 2][0], wallPoints [wallPoints.length / 2][1]); 
    return containsShapeAtWithMargin(endLine, x, y, margin);
  }

  private boolean containsShapeAtWithMargin(Shape shape, float x, float y, float margin) {
    if (margin == 0) {
      return shape.contains(x, y);
    } else {
      return shape.intersects(x - margin, y - margin, 2 * margin, 2 * margin);
    }
  }

  private Shape getShape(boolean includeBaseboards) {
    float [][] wallPoints = getPoints(includeBaseboards);
    GeneralPath wallPath = new GeneralPath();
    wallPath.moveTo(wallPoints [0][0], wallPoints [0][1]);
    for (int i = 1; i < wallPoints.length; i++) {
      wallPath.lineTo(wallPoints [i][0], wallPoints [i][1]);
    }
    wallPath.closePath();
    return wallPath;
  }

  public static List<Wall> clone(List<Wall> walls) {
    ArrayList<Wall> wallsCopy = new ArrayList<Wall>(walls.size());
    for (Wall wall : walls) {
      wallsCopy.add(wall.clone());      
    }
    for (int i = 0; i < walls.size(); i++) {
      Wall wall = walls.get(i);
      int wallAtStartIndex = walls.indexOf(wall.getWallAtStart());
      if (wallAtStartIndex != -1) {
        wallsCopy.get(i).setWallAtStart(wallsCopy.get(wallAtStartIndex));
      }
      int wallAtEndIndex = walls.indexOf(wall.getWallAtEnd());
      if (wallAtEndIndex != -1) {
        wallsCopy.get(i).setWallAtEnd(wallsCopy.get(wallAtEndIndex));
      }
    }
    return wallsCopy;
  }
  public void move(float dx, float dy) {
    setXStart(getXStart() + dx);
    setYStart(getYStart() + dy);
    setXEnd(getXEnd() + dx);
    setYEnd(getYEnd() + dy);
  }

  public Wall clone() {
    Wall clone = (Wall)super.clone();
    clone.propertyChangeSupport = new PropertyChangeSupport(clone);
    clone.wallAtStart = null;
    clone.wallAtEnd = null;
    clone.level = null;
    clone.pointsCache = null;
    clone.pointsIncludingBaseboardsCache = null;
    return clone;
  }
}
