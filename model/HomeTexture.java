package com.eteks.homeview3d.model;

import java.io.Serializable;

public class HomeTexture implements TextureImage, Serializable {
  private static final long serialVersionUID = 1L;
  
  private final String catalogId;
  private final String name;
  private final Content image;
  private final float width;
  private final float height;
  private final float angle;
  private final boolean leftToRightOriented;

  public HomeTexture(TextureImage texture) {
    this(texture, 0);
  }

  public HomeTexture(TextureImage texture, float angle) {
    this(texture, angle, true);
  }

  public HomeTexture(TextureImage texture, float angle, boolean leftToRightOriented) {
    this.name = texture.getName();
    this.image = texture.getImage();
    this.width = texture.getWidth();
    this.height = texture.getHeight();
    this.angle = angle;
    this.leftToRightOriented = leftToRightOriented; 
    if (texture instanceof HomeTexture) {
      this.catalogId = ((HomeTexture)texture).getCatalogId();
    } else if (texture instanceof CatalogTexture) {
      this.catalogId = ((CatalogTexture)texture).getId();
    } else {
      this.catalogId = null;
    }
  }

  public String getCatalogId() {
    return this.catalogId;
  }

  public String getName() {
    return this.name;
  }

  public Content getImage() {
    return this.image;
  }

  public float getWidth() {
    return this.width;
  }

  public float getHeight() {
    return this.height;
  }

  public float getAngle() {
    return this.angle;
  }

  public boolean isLeftToRightOriented() {
    return this.leftToRightOriented;
  }

  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof HomeTexture) {
      HomeTexture texture = (HomeTexture)obj;
      return (texture.name == this.name 
              || texture.name != null && texture.name.equals(this.name))
          && (texture.image == this.image 
              || texture.image != null && texture.image.equals(this.image))
          && texture.width == this.width
          && texture.height == this.height
          && texture.leftToRightOriented == this.leftToRightOriented
          && texture.angle == this.angle;
    } else {
      return false;
    }
  }

  public int hashCode() {
    return (this.name != null  ? this.name.hashCode()   : 0)
        + (this.image != null  ? this.image.hashCode()  : 0)
        + Float.floatToIntBits(this.width)
        + Float.floatToIntBits(this.height)
        + Float.floatToIntBits(this.angle);
  }
}
