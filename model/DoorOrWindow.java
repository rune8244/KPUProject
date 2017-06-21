package com.eteks.homeview3d.model;

public interface DoorOrWindow extends PieceOfFurniture {

  public abstract float getWallThickness();
  
  public abstract float getWallDistance();

  public abstract Sash [] getSashes();

  public abstract String getCutOutShape();
}
