package com.eteks.homeview3d.model;

import java.math.BigDecimal;

public interface PieceOfFurniture {

  public abstract String getName();
  public abstract String getDescription();
  public String getInformation();
  public abstract float getDepth();
  public abstract float getHeight();
  public abstract float getWidth();
  public abstract float getElevation();
  public abstract float getDropOnTopElevation();
  public abstract boolean isMovable();
  public abstract boolean isDoorOrWindow();
  public abstract Content getIcon();
  public abstract Content getPlanIcon();
  public abstract Content getModel();
  public float [][] getModelRotation();
  public String getStaircaseCutOutShape();
  public String getCreator();
  public abstract boolean isBackFaceShown();
  public abstract Integer getColor();
  public abstract boolean isResizable();
  public abstract boolean isDeformable();
  public abstract boolean isTexturable();
  public abstract BigDecimal getPrice();
  public abstract BigDecimal getValueAddedTaxPercentage();
  public abstract String getCurrency();
}