package com.eteks.homeview3d.model;

import java.io.Serializable;

public class HomeMaterial implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private final String      name;
  private final String      key;
  private final Integer     color;
  private final HomeTexture texture;
  private final Float       shininess;

  public HomeMaterial(String name, Integer color, HomeTexture texture, Float shininess) {
    this(name, null, color, texture, shininess);
  }

  public HomeMaterial(String name, String key, Integer color, HomeTexture texture, Float shininess) {
    this.name = name;
    this.key = key;
    this.color = color;
    this.texture = texture;
    this.shininess = shininess;
  }

  public String getName() {
    return this.name;
  }

  public String getKey() {
    return this.key;
  }

  public Integer getColor() {
    return this.color;
  }

  public HomeTexture getTexture() {
    return this.texture;
  }

  public Float getShininess() {
    return this.shininess;
  }
}
