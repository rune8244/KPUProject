package com.eteks.homeview3d.model;

public enum AspectRatio {
  FREE_RATIO(null), 
  VIEW_3D_RATIO(null), 
  RATIO_4_3(4f / 3), 
  RATIO_3_2(1.5f), 
  RATIO_16_9(16f / 9),
  RATIO_2_1(2f / 1f),
  SQUARE_RATIO(1f);
  
  private final Float value;
  
  private AspectRatio(Float value) {
    this.value = value;
  }    
  
  /* 폭,높이 비율 반환 */
 
 public Float getValue() {
    return value;
  }
}