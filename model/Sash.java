package com.eteks.homeview3d.model;

import java.io.Serializable;

public class Sash implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private final float xAxis;
  private final float yAxis;
  private final float width;
  private final float startAngle;
  private final float endAngle;
  
  /**
   * Ã¢ »þ½Ã »ý¼º.
   */
  public Sash(float xAxis, float yAxis, 
              float width, 
              float startAngle,
              float endAngle) {
    this.xAxis = xAxis;
    this.yAxis = yAxis;
    this.width = width;
    this.startAngle = startAngle;
    this.endAngle = endAngle;
  }

  public float getXAxis() {
    return this.xAxis;
  }

  public float getYAxis() {
    return this.yAxis;
  }

  public float getWidth() {
    return this.width;
  }

  public float getStartAngle() {
    return this.startAngle;
  }    

  public float getEndAngle() {
    return this.endAngle;
  }    
}