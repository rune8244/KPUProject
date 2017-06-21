package com.eteks.homeview3d.model;

import java.awt.geom.Point2D;
import java.io.Serializable;

public class BackgroundImage implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private final Content image;
  private final float   scaleDistance;
  private final float   scaleDistanceXStart;
  private final float   scaleDistanceYStart;
  private final float   scaleDistanceXEnd;
  private final float   scaleDistanceYEnd;
  private final float   xOrigin;
  private final float   yOrigin;
  private final boolean invisible; 
  
  /**
   * ��׶��� �̹��� ����
   */
  public BackgroundImage(Content image, float scaleDistance, 
                         float scaleDistanceXStart, float scaleDistanceYStart, 
                         float scaleDistanceXEnd, float scaleDistanceYEnd, 
                         float xOrigin, float yOrigin) {
    this(image, scaleDistance, scaleDistanceXStart, 
        scaleDistanceYStart, scaleDistanceXEnd, scaleDistanceYEnd, xOrigin, yOrigin, true);
  }

  public BackgroundImage(Content image, float scaleDistance, 
                         float scaleDistanceXStart, float scaleDistanceYStart, 
                         float scaleDistanceXEnd, float scaleDistanceYEnd, 
                         float xOrigin, float yOrigin, boolean visible) {
    this.image = image;
    this.scaleDistance = scaleDistance;
    this.scaleDistanceXStart = scaleDistanceXStart;
    this.scaleDistanceYStart = scaleDistanceYStart;
    this.scaleDistanceXEnd = scaleDistanceXEnd;
    this.scaleDistanceYEnd = scaleDistanceYEnd;
    this.xOrigin = xOrigin;
    this.yOrigin = yOrigin;
    this.invisible = !visible;
  }

  /**
   * ��׶��� �̹��� ���� 
   */
  public Content getImage() {
    return this.image;
  }
  
  /**
   * �̹��� ���� ��꿡 ���� �Ÿ� ��ȯ
   */
  public float getScaleDistance() {
    return this.scaleDistance;
  }

  /**
   * �̹��� ���� ��꿡 ���� �������� ���� ��ǥ ��ȯ
   */
  public float getScaleDistanceXStart() {
    return this.scaleDistanceXStart;
  }

  /**
   * �̹��� ���� ��꿡 ���� �������� ���� ��ǥ ��ȯ
   */
  public float getScaleDistanceYStart() {
    return this.scaleDistanceYStart;
  }

  /**
   * �̹��� ���� ��꿡 ���� ������ ���� ��ǥ ��ȯ
   */
  public float getScaleDistanceXEnd() {
    return this.scaleDistanceXEnd;
  }

  /**
   *  �̹��� ���� ��꿡 ���� ������ ���� ��ǥ ��ȯ
   */
  public float getScaleDistanceYEnd() {
    return this.scaleDistanceYEnd;
  }

  /**
   * �̹����� ������ ��ȯ
   */
  public float getScale() {
    return getScale(this.scaleDistance,
        this.scaleDistanceXStart, this.scaleDistanceYStart, 
        this.scaleDistanceXEnd, this.scaleDistanceYEnd);
  }
  
  /**
   * ������ 
   */
  public static float getScale(float scaleDistance, 
                               float scaleDistanceXStart, float scaleDistanceYStart,
                               float scaleDistanceXEnd, float scaleDistanceYEnd) {
    return (float)(scaleDistance 
        / Point2D.distance(scaleDistanceXStart, scaleDistanceYStart, 
                           scaleDistanceXEnd, scaleDistanceYEnd));
  }
  
  /**
   * �̹����� ���� ���� ��ǥ ��ȯ
   */
  public float getXOrigin() {
    return this.xOrigin;
  }
  
  /**
   * �̹����� ���� ���� ��ǥ ��ȯ
   */
  public float getYOrigin() {
    return this.yOrigin;
  }

  /**
   * ��ȹ��� �̹��� ��ȯ �� true�ڵ� ��ȯ
   */
  public boolean isVisible() {
    return !this.invisible;
  }
}