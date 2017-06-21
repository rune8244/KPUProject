package com.eteks.homeview3d.model;

import java.io.Serializable;

public interface TextureImage extends Serializable {

  public abstract String getName();

  public abstract Content getImage();

  public abstract float getWidth();

  public abstract float getHeight();

}