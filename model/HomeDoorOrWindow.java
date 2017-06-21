package com.eteks.homeview3d.model;

import java.io.IOException;
import java.io.ObjectInputStream;

public class HomeDoorOrWindow extends HomePieceOfFurniture implements DoorOrWindow {
  private static final long serialVersionUID = 1L;

  private final float   wallThickness;
  private final float   wallDistance;
  private final Sash [] sashes;
  private String  cutOutShape;
  private boolean boundToWall;

  public HomeDoorOrWindow(DoorOrWindow doorOrWindow) {
    super(doorOrWindow);
    this.wallThickness = doorOrWindow.getWallThickness();
    this.wallDistance = doorOrWindow.getWallDistance();
    this.sashes = doorOrWindow.getSashes();
    this.cutOutShape = doorOrWindow.getCutOutShape();
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    this.cutOutShape = "M0,0 v1 h1 v-1 z";
    in.defaultReadObject();
  }
  
  public float getWallThickness() {
    return this.wallThickness;
  }

  public float getWallDistance() {
    return this.wallDistance;
  }

  public Sash [] getSashes() {
    if (this.sashes.length == 0) {
      return this.sashes;
    } else {
      return this.sashes.clone();
    }
  }

  public String getCutOutShape() {
    return this.cutOutShape;
  }

  public boolean isBoundToWall() {
    return this.boundToWall;
  }

  public void setBoundToWall(boolean boundToWall) {
    this.boundToWall = boundToWall;
  }

  public void setX(float x) {
    if (getX() != x) {
      this.boundToWall = false;
    }
    super.setX(x);
  }

  public void setY(float y) {
    if (getY() != y) {
      this.boundToWall = false;
    }
    super.setY(y);
  }

  public void setAngle(float angle) {
    if (getAngle() != angle) {
      this.boundToWall = false;
    }
    super.setAngle(angle);
  }

  public void setDepth(float depth) {
    if (getDepth() != depth) {
      this.boundToWall = false;
    }
    super.setDepth(depth);
  }

  public boolean isDoorOrWindow() {
    return true;
  }

  public HomeDoorOrWindow clone() {
    HomeDoorOrWindow clone = (HomeDoorOrWindow)super.clone();
    clone.boundToWall = false;
    return clone;    
  }
}
