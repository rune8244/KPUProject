package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.ref.WeakReference;
import java.util.List;

import com.eteks.homeview3d.model.AspectRatio;
import com.eteks.homeview3d.model.Camera;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeEnvironment;
import com.eteks.homeview3d.model.UserPreferences;

public class VideoController implements Controller {
  public enum Property {ASPECT_RATIO, FRAME_RATE, WIDTH, HEIGHT, QUALITY, CAMERA_PATH, TIME, CEILING_LIGHT_COLOR}
  
  private final Home                  home;
  private final UserPreferences       preferences;
  private final ViewFactory           viewFactory;
  private final ContentManager        contentManager;
  private final PropertyChangeSupport propertyChangeSupport;
  private DialogView                  videoView;
  
  private AspectRatio                 aspectRatio;
  private int                         frameRate;
  private int                         width;
  private int                         height;
  private int                         quality;
  private List<Camera>                cameraPath;
  private long                        time;
  private int                         ceilingLightColor;

  public VideoController(Home home,
                         UserPreferences preferences, 
                         ViewFactory viewFactory,
                         ContentManager contentManager) {
    this.home = home;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.contentManager = contentManager;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    
    updateProperties();
    home.getEnvironment().addPropertyChangeListener(HomeEnvironment.Property.CEILING_LIGHT_COLOR, new HomeEnvironmentChangeListener(this));
  }

  private static class HomeEnvironmentChangeListener implements PropertyChangeListener {
    private WeakReference<VideoController> videoController;
    
    public HomeEnvironmentChangeListener(VideoController videoController) {
      this.videoController = new WeakReference<VideoController>(videoController);
    }
    
    public void propertyChange(PropertyChangeEvent ev) {
      final VideoController controller = this.videoController.get();
      if (controller == null) {
        ((HomeEnvironment)ev.getSource()).removePropertyChangeListener(HomeEnvironment.Property.CEILING_LIGHT_COLOR, this);
      } else {
        controller.updateProperties();
      }
    }
  }

  public DialogView getView() {
    if (this.videoView == null) {
      this.videoView = this.viewFactory.createVideoView(this.home, this.preferences, this);
    }
    return this.videoView;
  }

  public void displayView(View parentView) {
    getView().displayView(parentView);
  }

  public ContentManager getContentManager() {
    return this.contentManager;
  }

  public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
  }

  public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
  }

  protected void updateProperties() {
    HomeEnvironment homeEnvironment = this.home.getEnvironment();
    setAspectRatio(homeEnvironment.getVideoAspectRatio());
    setFrameRate(homeEnvironment.getVideoFrameRate());
    setWidth(homeEnvironment.getVideoWidth(), false);
    setHeight(homeEnvironment.getVideoHeight(), false);
    setQuality(homeEnvironment.getVideoQuality());
    List<Camera> videoCameraPath = homeEnvironment.getVideoCameraPath();
    setCameraPath(videoCameraPath);
    setTime(videoCameraPath.isEmpty() 
        ? this.home.getCamera().getTime()
        : videoCameraPath.get(0).getTime());
    setCeilingLightColor(homeEnvironment.getCeillingLightColor());
  }
  
  public void setAspectRatio(AspectRatio aspectRatio) {
    if (this.aspectRatio != aspectRatio) {
      AspectRatio oldAspectRatio = this.aspectRatio;
      this.aspectRatio = aspectRatio;
      this.propertyChangeSupport.firePropertyChange(Property.ASPECT_RATIO.name(), oldAspectRatio, aspectRatio);
      this.home.getEnvironment().setVideoAspectRatio(this.aspectRatio);
      setHeight(Math.round(width / this.aspectRatio.getValue()), false);
    }
  }
  
  public AspectRatio getAspectRatio() {
    return this.aspectRatio;
  }

  public void setFrameRate(int frameRate) {
    if (this.frameRate != frameRate) {
      int oldFrameRate = this.frameRate;
      this.frameRate = frameRate;
      this.propertyChangeSupport.firePropertyChange(Property.QUALITY.name(), oldFrameRate, frameRate);
      this.home.getEnvironment().setVideoFrameRate(this.frameRate);
    }
  }
  
  public int getFrameRate() {
    return this.frameRate;
  }

  public void setWidth(int width) {
    setWidth(width, true);
  }
  
  private void setWidth(int width, boolean updateHeight) {
    if (this.width != width) {
      int oldWidth = this.width;
      this.width = width;
      this.propertyChangeSupport.firePropertyChange(Property.WIDTH.name(), oldWidth, width);
      if (updateHeight) {
        setHeight(Math.round(width / this.aspectRatio.getValue()), false);
      }
      this.home.getEnvironment().setVideoWidth(this.width);
    }
  }
  
  public int getWidth() {
    return this.width;
  }

  public void setHeight(int height) {
    setHeight(height, true);
  }
  
  private void setHeight(int height, boolean updateWidth) {
    if (this.height != height) {
      int oldHeight = this.height;
      this.height = height;
      this.propertyChangeSupport.firePropertyChange(Property.HEIGHT.name(), oldHeight, height);
      if (updateWidth) {
        setWidth(Math.round(height * this.aspectRatio.getValue()), false);
      }
    }
  }
  
  public int getHeight() {
    return this.height;
  }

  public void setQuality(int quality) {
    if (this.quality != quality) {
      int oldQuality = this.quality;
      this.quality = Math.min(quality, getQualityLevelCount() - 1);
      this.propertyChangeSupport.firePropertyChange(Property.QUALITY.name(), oldQuality, quality);
      this.home.getEnvironment().setVideoQuality(this.quality);
    }
  }
  
  public int getQuality() {
    return this.quality;
  }

  public int getQualityLevelCount() {
    return 4;
  }

  public List<Camera> getCameraPath() {
    return this.cameraPath;
  }
  
  public void setCameraPath(List<Camera> cameraPath) {
    if (this.cameraPath != cameraPath) {
      List<Camera> oldCameraPath = this.cameraPath;
      this.cameraPath = cameraPath;
      this.propertyChangeSupport.firePropertyChange(Property.CAMERA_PATH.name(), oldCameraPath, cameraPath);
      this.home.getEnvironment().setVideoCameraPath(this.cameraPath);
    }
  }

  public void setTime(long time) {
    if (this.time != time) {
      long oldTime = this.time;
      this.time = time;
      this.propertyChangeSupport.firePropertyChange(Property.TIME.name(), oldTime, time);
      this.home.getCamera().setTime(time);
    }
  }
  
  public long getTime() {
    return this.time;
  }

  public void setCeilingLightColor(int ceilingLightColor) {
    if (this.ceilingLightColor != ceilingLightColor) {
      int oldCeilingLightColor = this.ceilingLightColor;
      this.ceilingLightColor = ceilingLightColor;
      this.propertyChangeSupport.firePropertyChange(Property.CEILING_LIGHT_COLOR.name(), oldCeilingLightColor, ceilingLightColor);
      this.home.getEnvironment().setCeillingLightColor(ceilingLightColor);
    }
  }
  
  public int getCeilingLightColor() {
    return this.ceilingLightColor;
  }

  public void setVisualProperty(String propertyName,
                                Object propertyValue) {
    this.home.setVisualProperty(propertyName, propertyValue);
  }

  public void setHomeProperty(String propertyName,
                                String propertyValue) {
    this.home.setProperty(propertyName, propertyValue);
  }
}
