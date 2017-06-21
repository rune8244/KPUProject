package com.eteks.homeview3d.model;

import java.io.Serializable;

public class LightSource implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private final float x;
  private final float y;
  private final float z;
  private final int   color;
  private final Float diameter;

  public LightSource(float x, float y, float z, int color) {
    this(x, y, z, color, null);
  }

  public LightSource(float x, float y, float z, int color, Float diameter) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.color = color;
    this.diameter = diameter;
  }

  public float getX() {
    return this.x;
  }

  public float getY() {
    return this.y;
  }

  public float getZ() {
    return this.z;
  }
  
  public int getColor() {
    return this.color;
  }

  public Float getDiameter() {
    return this.diameter;
  }
}
