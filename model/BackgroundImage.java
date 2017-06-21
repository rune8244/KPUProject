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
   * 백그라운드 이미지 생성
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
   * 백그라운드 이미지 내용 
   */
  public Content getImage() {
    return this.image;
  }
  
  /**
   * 이미지 눈금 계산에 사용된 거리 반환
   */
  public float getScaleDistance() {
    return this.scaleDistance;
  }

  /**
   * 이미지 눈금 계산에 사용된 시작점의 가로 좌표 반환
   */
  public float getScaleDistanceXStart() {
    return this.scaleDistanceXStart;
  }

  /**
   * 이미지 눈금 계산에 사용된 시작점의 세로 좌표 반환
   */
  public float getScaleDistanceYStart() {
    return this.scaleDistanceYStart;
  }

  /**
   * 이미지 눈금 계산에 사용된 끝점의 가로 좌표 반환
   */
  public float getScaleDistanceXEnd() {
    return this.scaleDistanceXEnd;
  }

  /**
   *  이미지 눈금 계산에 사용된 끝점의 세로 좌표 반환
   */
  public float getScaleDistanceYEnd() {
    return this.scaleDistanceYEnd;
  }

  /**
   * 이미지의 스케일 반환
   */
  public float getScale() {
    return getScale(this.scaleDistance,
        this.scaleDistanceXStart, this.scaleDistanceYStart, 
        this.scaleDistanceXEnd, this.scaleDistanceYEnd);
  }
  
  /**
   * 나눗셈 
   */
  public static float getScale(float scaleDistance, 
                               float scaleDistanceXStart, float scaleDistanceYStart,
                               float scaleDistanceXEnd, float scaleDistanceYEnd) {
    return (float)(scaleDistance 
        / Point2D.distance(scaleDistanceXStart, scaleDistanceYStart, 
                           scaleDistanceXEnd, scaleDistanceYEnd));
  }
  
  /**
   * 이미지의 원점 가로 좌표 반환
   */
  public float getXOrigin() {
    return this.xOrigin;
  }
  
  /**
   * 이미지의 원점 세로 좌표 반환
   */
  public float getYOrigin() {
    return this.yOrigin;
  }

  /**
   * 계획대로 이미지 반환 시 true코드 반환
   */
  public boolean isVisible() {
    return !this.invisible;
  }
}