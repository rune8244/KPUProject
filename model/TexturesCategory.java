package com.eteks.homeview3d.model;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TexturesCategory implements Comparable<TexturesCategory> {
  private final String         name;
  private List<CatalogTexture> textures;
  
  private static final Collator  COMPARATOR = Collator.getInstance();

  public TexturesCategory(String name) {
    this.name = name;
    this.textures = new ArrayList<CatalogTexture>();
  }

  public String getName() {
    return this.name;
  }

  public List<CatalogTexture> getTextures() {
    return Collections.unmodifiableList(this.textures);
  }

  public int getTexturesCount() {
    return this.textures.size();
  }

  public CatalogTexture getTexture(int index) {
    return this.textures.get(index);
  }

  public int getIndexOfTexture(CatalogTexture texture) {
    return this.textures.indexOf(texture);
  }

  void add(CatalogTexture texture) {
    texture.setCategory(this);
    int index = Collections.binarySearch(this.textures, texture);
    if (index < 0) {
      index = -index - 1;
    } 
    this.textures.add(index, texture);    
  }

  void delete(CatalogTexture texture) {
    int textureIndex = this.textures.indexOf(texture);
    if (textureIndex == -1) {
      throw new IllegalArgumentException(
          this.name + " doesn't contain texture " + texture.getName());
    }
    this.textures = new ArrayList<CatalogTexture>(this.textures);
    this.textures.remove(textureIndex);
  }

  public boolean equals(Object obj) {
    return obj instanceof TexturesCategory
           && COMPARATOR.equals(this.name, ((TexturesCategory)obj).name);
  }

  public int hashCode() {
    return this.name.hashCode();
  }

  /**
   * 카테고리 이름과 매개 변수 이름 비교.
   */
  public int compareTo(TexturesCategory category) {
    return COMPARATOR.compare(this.name, category.name);
  }
}
