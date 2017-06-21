package com.eteks.homeview3d.j3d;

import javax.media.j3d.Material;
import javax.media.j3d.NodeComponent;


public class OBJMaterial extends Material {
  private Float   opticalDensity;
  private Integer illuminationModel;
  private Float   sharpness;


  public void setOpticalDensity(float opticalDensity) {
    this.opticalDensity = opticalDensity;
  }
  

  public float getOpticalDensity() {
    if (this.opticalDensity != null) {
      return this.opticalDensity;
    } else {
      throw new IllegalStateException("Optical density not set");
    }
  }

  public boolean isOpticalDensitySet() {
    return this.opticalDensity != null;
  }


  public void setIlluminationModel(int illuminationModel) {
    this.illuminationModel = illuminationModel;
  }
  
  
  public int getIlluminationModel() {
    if (this.illuminationModel != null) {
      return this.illuminationModel;
    } else {
      throw new IllegalStateException("Optical density not set");
    }
  }
  

  public boolean isIlluminationModelSet() {
    return this.illuminationModel != null;
  }
  

  public void setSharpness(float sharpness) {
    this.sharpness = sharpness;
  }
  
  public float getSharpness() {
    if (this.sharpness != null) {
      return this.sharpness;
    } else {
      throw new IllegalStateException("Sharpness not set");
    }
  }
  

  public boolean isSharpnessSet() {
    return this.sharpness != null;
  }


  @Override
  public NodeComponent cloneNodeComponent(boolean forceDuplicate) {
    OBJMaterial material = new OBJMaterial();
    material.duplicateNodeComponent(this, forceDuplicate);
    material.opticalDensity = this.opticalDensity;
    material.illuminationModel = this.illuminationModel;
    material.sharpness = this.sharpness;
    return material;
  }
}
