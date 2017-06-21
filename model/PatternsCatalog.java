package com.eteks.homeview3d.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PatternsCatalog {
  private List<TextureImage> patterns;

  public PatternsCatalog(List<TextureImage> patterns) {
    this.patterns = new ArrayList<TextureImage>(patterns);
  }

  public List<TextureImage> getPatterns() {
    return Collections.unmodifiableList(this.patterns);
  }

  public int getPatternsCount() {
    return this.patterns.size();
  }

  public TextureImage getPattern(int index) {
    return this.patterns.get(index);
  }

  public TextureImage getPattern(String name) {
    for (TextureImage pattern : patterns) {
      if (name.equals(pattern.getName())) {
        return pattern;
      }
    }
    throw new IllegalArgumentException("No pattern with name " + name);
  }
}
