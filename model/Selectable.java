package com.eteks.homeview3d.model;

public interface Selectable extends Cloneable {

  public abstract float [][] getPoints();

  public abstract boolean intersectsRectangle(float x0, float y0,
                                              float x1, float y1);

  public abstract boolean containsPoint(float x, float y,
                                        float margin);

  public abstract void move(float dx, float dy);

  public abstract Selectable clone();
}