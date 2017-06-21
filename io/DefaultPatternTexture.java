package com.eteks.homeview3d.io;

import java.io.IOException;
import java.io.ObjectInputStream;

import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.TextureImage;
import com.eteks.homeview3d.tools.ResourceURLContent;

class DefaultPatternTexture implements TextureImage {
  private static final long serialVersionUID = 1L;

  private final String      name;
  private transient Content image;
  
  public DefaultPatternTexture(String name) {
    this.name = name;
    this.image = new ResourceURLContent(DefaultPatternTexture.class, "resources/patterns/" + this.name + ".png");
  }
  
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    try {
      this.image = new ResourceURLContent(DefaultPatternTexture.class, "resources/patterns/" + this.name + ".png");
    } catch (IllegalArgumentException ex) {
      this.image = new ResourceURLContent(DefaultPatternTexture.class, "resources/patterns/foreground.png");
    }
  }

  public String getName() {
    return this.name;
  }

  public Content getImage() {
    return this.image;
  }
  
  public float getWidth() {
    return 10;
  }
  
  public float getHeight() {
    return 10;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    } else if (obj instanceof DefaultPatternTexture) {
      DefaultPatternTexture pattern = (DefaultPatternTexture)obj;
      return pattern.name.equals(this.name);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return this.name.hashCode();
  }
}