package com.eteks.homeview3d.model;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class Baseboard implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private final float       thickness;
  private final float       height;
  private final Integer     color;
  private final HomeTexture texture;
  
  private static final List<WeakReference<Baseboard>> baseboardsCache = new ArrayList<WeakReference<Baseboard>>(); 

  /**
   베이스 보드 생성
   */
  public Baseboard(float thickness, float height, Integer color, HomeTexture texture) {
    this(height, thickness, color, texture, true);
  }

  private Baseboard(float thickness, float height, Integer color, HomeTexture texture, boolean cached) {
    this.height = height;
    this.thickness = thickness;
    this.color = color;
    this.texture = texture;
    
    if (cached) {
      baseboardsCache.add(new WeakReference<Baseboard>(this));
    }
  }

  /**
   * 속성 읽고 캐시 업데이트
   */
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    
    baseboardsCache.add(new WeakReference<Baseboard>(this));
  }

  /**
   * 주어진 변수에 일치하는 클래스의 인스턴스 반환
   */
  public static Baseboard getInstance(float thickness, float height, 
                                      Integer color, HomeTexture texture) {
    Baseboard baseboard = new Baseboard(thickness, height, color, texture, false);
    for (int i = baseboardsCache.size() - 1; i >= 0; i--) {
      Baseboard cachedBaseboard = baseboardsCache.get(i).get();
      if (cachedBaseboard == null) {
        baseboardsCache.remove(i);
      } else if (cachedBaseboard.equals(baseboard)) {
        return baseboard;
      }
    }
    baseboardsCache.add(new WeakReference<Baseboard>(baseboard));
    return baseboard;
  }

  /**
   * 베이스보드 두께 반환.
   */
  public float getThickness() {
    return this.thickness;
  }

  /**
   * 베이스보드 높이 반환. 
   */
  public float getHeight() {
    return this.height;
  }

  /**
   * 베이스보드 색 반환.
   */
  public Integer getColor() {
    return this.color;
  }

  /**
   * 베이스보드 텍스처 반환.
   */
  public HomeTexture getTexture() {
    return this.texture;
  }

  public boolean equals(Object object) {
    if (object instanceof Baseboard) {
      Baseboard baseboard = (Baseboard)object;
      return baseboard.thickness == this.thickness
          && baseboard.height == this.height
          && (baseboard.color == this.color
              || baseboard.color != null && baseboard.color.equals(this.color))
          && (baseboard.texture == this.texture
              || baseboard.texture != null && baseboard.texture.equals(this.texture));
    }
    return false;
  }
  
  /**
   * 해시 코드 반환.
   */
  public int hashCode() {
    int hashCode = Float.floatToIntBits(this.thickness)
        + Float.floatToIntBits(this.height);
    if (this.color != null) {
      hashCode += this.color.hashCode();
    }
    if (this.texture != null) {
      hashCode += this.texture.hashCode();
    }
    return hashCode;
  }
}
