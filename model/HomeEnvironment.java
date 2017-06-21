package com.eteks.homeview3d.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeEnvironment implements Serializable, Cloneable {
  private static final long serialVersionUID = 1L;

  public enum Property {OBSERVER_CAMERA_ELEVATION_ADJUSTED, SKY_COLOR, SKY_TEXTURE, GROUND_COLOR, GROUND_TEXTURE, LIGHT_COLOR, CEILING_LIGHT_COLOR, 
                        WALLS_ALPHA, DRAWING_MODE, SUBPART_SIZE_UNDER_LIGHT, ALL_LEVELS_VISIBLE,
                        PHOTO_WIDTH, PHOTO_HEIGHT, PHOTO_ASPECT_RATIO, PHOTO_QUALITY, 
                        VIDEO_WIDTH, VIDEO_ASPECT_RATIO, VIDEO_QUALITY, VIDEO_FRAME_RATE, VIDEO_CAMERA_PATH};

  public enum DrawingMode {
    FILL, OUTLINE, FILL_AND_OUTLINE
  }
  
  private boolean                         observerCameraElevationAdjusted;
  private int                             groundColor;
  private HomeTexture                     groundTexture;
  private int                             skyColor;
  private HomeTexture                     skyTexture;
  private int                             lightColor;
  private int                             ceilingLightColor;
  private float                           wallsAlpha;
  private DrawingMode                     drawingMode;
  private float                           subpartSizeUnderLight;      
  private boolean                         allLevelsVisible; 
  private int                             photoWidth;
  private int                             photoHeight;
  private transient AspectRatio           photoAspectRatio;
  private String                          photoAspectRatioName;
  private int                             photoQuality;
  private int                             videoWidth;
  private transient AspectRatio           videoAspectRatio;
  private String                          videoAspectRatioName;
  private int                             videoQuality;
  private int                             videoFrameRate;
  private List<Camera>                    cameraPath;
  private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  /**
   * 기본 환경 생성.
   */
  public HomeEnvironment() {
    this(0xA8A8A8, // 바탕색
         null,     // 텍스쳐
         0xCCE4FC, // 하늘색
         0xD0D0D0, // 밝은 색
         0);       // 벽
  }

  /**
   * 매개 변수로 집 환경 생성.
   */
  public HomeEnvironment(int groundColor,
                         HomeTexture groundTexture, int skyColor,
                         int lightColor, float wallsAlpha) {
    this(groundColor, groundTexture, skyColor, null,
        lightColor, wallsAlpha);
  }

  public HomeEnvironment(int groundColor, HomeTexture groundTexture, 
                         int skyColor, HomeTexture skyTexture,
                         int lightColor, float wallsAlpha) {
    this.observerCameraElevationAdjusted = true;
    this.groundColor = groundColor;
    this.groundTexture = groundTexture;
    this.skyColor = skyColor;
    this.skyTexture = skyTexture;
    this.lightColor = lightColor;
    this.ceilingLightColor = 0xD0D0D0;
    this.wallsAlpha = wallsAlpha;
    this.drawingMode = DrawingMode.FILL;
    this.photoWidth = 400;
    this.photoHeight = 300;
    this.photoAspectRatio = AspectRatio.VIEW_3D_RATIO;
    this.videoWidth = 320;
    this.videoAspectRatio = AspectRatio.RATIO_4_3;
    this.videoFrameRate = 25;
    this.cameraPath = Collections.emptyList();
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    this.ceilingLightColor = 0xD0D0D0;
    this.photoWidth = 400;
    this.photoHeight = 300;
    this.photoAspectRatio = AspectRatio.VIEW_3D_RATIO;
    this.videoWidth = 320;
    this.videoAspectRatio = AspectRatio.RATIO_4_3;
    this.videoFrameRate = 25;
    this.cameraPath = Collections.emptyList();
    in.defaultReadObject();
    try {
      if (this.photoAspectRatioName != null) {
        this.photoAspectRatio = AspectRatio.valueOf(this.photoAspectRatioName);
      }
    } catch (IllegalArgumentException ex) {
    }
    try { 
      if (this.videoAspectRatioName != null) {
        this.videoAspectRatio = AspectRatio.valueOf(this.videoAspectRatioName);
      }
    } catch (IllegalArgumentException ex) {
    }
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {
    this.photoAspectRatioName = this.photoAspectRatio.name();
    this.videoAspectRatioName = this.videoAspectRatio.name();
    out.defaultWriteObject();
  }

  public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
  }

  public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
  }

  public boolean isObserverCameraElevationAdjusted() {
    return this.observerCameraElevationAdjusted;
  }

  public void setObserverCameraElevationAdjusted(boolean observerCameraElevationAdjusted) {
    if (this.observerCameraElevationAdjusted != observerCameraElevationAdjusted) {
      this.observerCameraElevationAdjusted = observerCameraElevationAdjusted;
      this.propertyChangeSupport.firePropertyChange(Property.OBSERVER_CAMERA_ELEVATION_ADJUSTED.name(), 
          !observerCameraElevationAdjusted, observerCameraElevationAdjusted);
    }
  }

  public int getGroundColor() {
    return this.groundColor;
  }

  public void setGroundColor(int groundColor) {
    if (groundColor != this.groundColor) {
      int oldGroundColor = this.groundColor;
      this.groundColor = groundColor;
      this.propertyChangeSupport.firePropertyChange(
          Property.GROUND_COLOR.name(), oldGroundColor, groundColor);
    }
  }

  public HomeTexture getGroundTexture() {
    return this.groundTexture;
  }

  public void setGroundTexture(HomeTexture groundTexture) {
    if (groundTexture != this.groundTexture) {
      HomeTexture oldGroundTexture = this.groundTexture;
      this.groundTexture = groundTexture;
      this.propertyChangeSupport.firePropertyChange(
          Property.GROUND_TEXTURE.name(), oldGroundTexture, groundTexture);
    }
  }

  public int getSkyColor() {
    return this.skyColor;
  }

  public void setSkyColor(int skyColor) {
    if (skyColor != this.skyColor) {
      int oldSkyColor = this.skyColor;
      this.skyColor = skyColor;
      this.propertyChangeSupport.firePropertyChange(
          Property.SKY_COLOR.name(), oldSkyColor, skyColor);
    }
  }

  public HomeTexture getSkyTexture() {
    return this.skyTexture;
  }

  public void setSkyTexture(HomeTexture skyTexture) {
    if (skyTexture != this.skyTexture) {
      HomeTexture oldSkyTexture = this.skyTexture;
      this.skyTexture = skyTexture;
      this.propertyChangeSupport.firePropertyChange(
          Property.SKY_TEXTURE.name(), oldSkyTexture, skyTexture);
    }
  }

  public int getLightColor() {
    return this.lightColor;
  }

  public void setLightColor(int lightColor) {
    if (lightColor != this.lightColor) {
      int oldLightColor = this.lightColor;
      this.lightColor = lightColor;
      this.propertyChangeSupport.firePropertyChange(
          Property.LIGHT_COLOR.name(), oldLightColor, lightColor);
    }
  }

  public int getCeillingLightColor() {
    return this.ceilingLightColor;
  }

  public void setCeillingLightColor(int ceilingLightColor) {
    if (ceilingLightColor != this.ceilingLightColor) {
      int oldCeilingLightColor = this.ceilingLightColor;
      this.ceilingLightColor = ceilingLightColor;
      this.propertyChangeSupport.firePropertyChange(
          Property.CEILING_LIGHT_COLOR.name(), oldCeilingLightColor, ceilingLightColor);
    }
  }

  public float getWallsAlpha() {
    return this.wallsAlpha;
  }

  public void setWallsAlpha(float wallsAlpha) {
    if (wallsAlpha != this.wallsAlpha) {
      float oldWallsAlpha = this.wallsAlpha;
      this.wallsAlpha = wallsAlpha;
      this.propertyChangeSupport.firePropertyChange(
          Property.WALLS_ALPHA.name(), oldWallsAlpha, wallsAlpha);
    }
  }

  public DrawingMode getDrawingMode() {
    return this.drawingMode;
  }

  public void setDrawingMode(DrawingMode drawingMode) {
    if (drawingMode != this.drawingMode) {
      DrawingMode oldDrawingMode = this.drawingMode;
      this.drawingMode = drawingMode;
      this.propertyChangeSupport.firePropertyChange(
          Property.DRAWING_MODE.name(), oldDrawingMode, drawingMode);
    }
  }

  public float getSubpartSizeUnderLight() {
    return this.subpartSizeUnderLight;
  }

  public void setSubpartSizeUnderLight(float subpartSizeUnderLight) {
    if (subpartSizeUnderLight != this.subpartSizeUnderLight) {
      float oldSubpartWidthUnderLight = this.subpartSizeUnderLight;
      this.subpartSizeUnderLight = subpartSizeUnderLight;
      this.propertyChangeSupport.firePropertyChange(
          Property.SUBPART_SIZE_UNDER_LIGHT.name(), oldSubpartWidthUnderLight, subpartSizeUnderLight);
    }
  }

  public boolean isAllLevelsVisible() {
    return this.allLevelsVisible;
  }

  public void setAllLevelsVisible(boolean allLevelsVisible) {
    if (allLevelsVisible != this.allLevelsVisible) {
      this.allLevelsVisible = allLevelsVisible;
      this.propertyChangeSupport.firePropertyChange(
          Property.ALL_LEVELS_VISIBLE.name(), !allLevelsVisible, allLevelsVisible);
    }
  }

  public int getPhotoWidth() {
    return this.photoWidth;
  }

  public void setPhotoWidth(int photoWidth) {
    if (this.photoWidth != photoWidth) {
      int oldPhotoWidth = this.photoWidth;
      this.photoWidth = photoWidth;
      this.propertyChangeSupport.firePropertyChange(Property.PHOTO_WIDTH.name(), 
          oldPhotoWidth, photoWidth);
    }
  }
  
  public int getPhotoHeight() {
    return this.photoHeight;
  }

  public void setPhotoHeight(int photoHeight) {
    if (this.photoHeight != photoHeight) {
      int oldPhotoHeight = this.photoHeight;
      this.photoHeight = photoHeight;
      this.propertyChangeSupport.firePropertyChange(Property.PHOTO_HEIGHT.name(), 
          oldPhotoHeight, photoHeight);
    }
  }

  public AspectRatio getPhotoAspectRatio() {
    return this.photoAspectRatio;
  }

  public void setPhotoAspectRatio(AspectRatio photoAspectRatio) {
    if (this.photoAspectRatio != photoAspectRatio) {
      AspectRatio oldPhotoAspectRatio = this.photoAspectRatio;
      this.photoAspectRatio = photoAspectRatio;
      this.propertyChangeSupport.firePropertyChange(Property.PHOTO_ASPECT_RATIO.name(), 
          oldPhotoAspectRatio, photoAspectRatio);
    }
  }

  public int getPhotoQuality() {
    return this.photoQuality;
  }

  public void setPhotoQuality(int photoQuality) {
    if (this.photoQuality != photoQuality) {
      int oldPhotoQuality = this.photoQuality;
      this.photoQuality = photoQuality;
      this.propertyChangeSupport.firePropertyChange(Property.PHOTO_QUALITY.name(), 
          oldPhotoQuality, photoQuality);
    }
  }

  public int getVideoWidth() {
    return this.videoWidth;
  }

  public void setVideoWidth(int videoWidth) {
    if (this.videoWidth != videoWidth) {
      int oldVideoWidth = this.videoWidth;
      this.videoWidth = videoWidth;
      this.propertyChangeSupport.firePropertyChange(Property.VIDEO_WIDTH.name(), 
          oldVideoWidth, videoWidth);
    }
  }

  public int getVideoHeight() {
    return Math.round(getVideoWidth() / getVideoAspectRatio().getValue());
  }

  public AspectRatio getVideoAspectRatio() {
    return this.videoAspectRatio;
  }

  public void setVideoAspectRatio(AspectRatio videoAspectRatio) {
    if (this.videoAspectRatio != videoAspectRatio) {
      if (videoAspectRatio.getValue() == null) {
        throw new IllegalArgumentException("Unsupported aspect ratio " + videoAspectRatio);
      }
      AspectRatio oldVideoAspectRatio = this.videoAspectRatio;
      this.videoAspectRatio = videoAspectRatio;
      this.propertyChangeSupport.firePropertyChange(Property.VIDEO_ASPECT_RATIO.name(), 
          oldVideoAspectRatio, videoAspectRatio);
    }
  }
  
  public int getVideoQuality() {
    return this.videoQuality;
  }

  public void setVideoQuality(int videoQuality) {
    if (this.videoQuality != videoQuality) {
      int oldVideoQuality = this.videoQuality;
      this.videoQuality = videoQuality;
      this.propertyChangeSupport.firePropertyChange(Property.VIDEO_QUALITY.name(), 
          oldVideoQuality, videoQuality);
    }
  }
  
  public int getVideoFrameRate() {
    return this.videoFrameRate;
  }

  public void setVideoFrameRate(int videoFrameRate) {
    if (this.videoFrameRate != videoFrameRate) {
      int oldVideoFrameRate = this.videoFrameRate;
      this.videoFrameRate = videoFrameRate;
      this.propertyChangeSupport.firePropertyChange(Property.VIDEO_FRAME_RATE.name(), 
          oldVideoFrameRate, videoFrameRate);
    }
  }

  public List<Camera> getVideoCameraPath() {
    return Collections.unmodifiableList(this.cameraPath);
  }

  public void setVideoCameraPath(List<Camera> cameraPath) {
    if (this.cameraPath != cameraPath) {
      List<Camera> oldCameraPath = this.cameraPath;
      if (cameraPath != null) {
        this.cameraPath = new ArrayList<Camera>(cameraPath);
      } else {
        this.cameraPath = Collections.emptyList();
      }
      this.propertyChangeSupport.firePropertyChange(Property.VIDEO_CAMERA_PATH.name(), oldCameraPath, cameraPath);
    }
  }

  public HomeEnvironment clone() {
    try {
      HomeEnvironment clone = (HomeEnvironment)super.clone();
      clone.cameraPath = new ArrayList<Camera>(this.cameraPath.size());
      for (Camera camera : this.cameraPath) {
        clone.cameraPath.add(camera.clone());
      }
      clone.propertyChangeSupport = new PropertyChangeSupport(clone);
      return clone;
    } catch (CloneNotSupportedException ex) {
      throw new IllegalStateException("Super class isn't cloneable"); 
    }
  }
}