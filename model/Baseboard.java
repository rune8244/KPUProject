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
   ���̽� ���� ����
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
   * �Ӽ� �а� ĳ�� ������Ʈ
   */
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    
    baseboardsCache.add(new WeakReference<Baseboard>(this));
  }

  /**
   * �־��� ������ ��ġ�ϴ� Ŭ������ �ν��Ͻ� ��ȯ
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
   * ���̽����� �β� ��ȯ.
   */
  public float getThickness() {
    return this.thickness;
  }

  /**
   * ���̽����� ���� ��ȯ. 
   */
  public float getHeight() {
    return this.height;
  }

  /**
   * ���̽����� �� ��ȯ.
   */
  public Integer getColor() {
    return this.color;
  }

  /**
   * ���̽����� �ؽ�ó ��ȯ.
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
   * �ؽ� �ڵ� ��ȯ.
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
