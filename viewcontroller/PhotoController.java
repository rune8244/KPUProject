package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.ref.WeakReference;

import com.eteks.homeview3d.model.AspectRatio;
import com.eteks.homeview3d.model.Camera;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.UserPreferences;

public class PhotoController extends AbstractPhotoController {
  public enum Property {TIME, LENS}
  
  private final Home                  home;
  private final UserPreferences       preferences;
  private final ViewFactory           viewFactory;
  private final PropertyChangeSupport propertyChangeSupport;
  private final CameraChangeListener  cameraChangeListener;
  private DialogView                  photoView;
  
  private long                        time;
  private Camera.Lens                 lens;  

  public PhotoController(Home home,
                         UserPreferences preferences, 
                         View view3D, ViewFactory viewFactory,
                         ContentManager contentManager) {
    super(home, preferences, view3D, contentManager);
    this.home = home;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    
    this.cameraChangeListener = new CameraChangeListener(this);
    home.getCamera().addPropertyChangeListener(this.cameraChangeListener);
    home.addPropertyChangeListener(Home.Property.CAMERA, new HomeCameraChangeListener(this));
    updateProperties();
  }

  private static class HomeCameraChangeListener implements PropertyChangeListener {
    private WeakReference<PhotoController> photoController;
    
    public HomeCameraChangeListener(PhotoController photoController) {
      this.photoController = new WeakReference<PhotoController>(photoController);
    }
    
    public void propertyChange(PropertyChangeEvent ev) {
      final PhotoController controller = this.photoController.get();
      if (controller == null) {
        ((Home)ev.getSource()).removePropertyChangeListener(Home.Property.CAMERA, this);
      } else {
        ((Camera)ev.getOldValue()).removePropertyChangeListener(controller.cameraChangeListener);
        controller.updateProperties();
        ((Camera)ev.getNewValue()).addPropertyChangeListener(controller.cameraChangeListener);
      }
    }
  }

  private static class CameraChangeListener implements PropertyChangeListener {
    private WeakReference<AbstractPhotoController> photoController;
    
    public CameraChangeListener(AbstractPhotoController photoController) {
      this.photoController = new WeakReference<AbstractPhotoController>(photoController);
    }
    
    public void propertyChange(PropertyChangeEvent ev) {
      final AbstractPhotoController controller = this.photoController.get();
      if (controller == null) {
        ((Camera)ev.getSource()).removePropertyChangeListener(this);
      } else {
        controller.updateProperties();
      }
    }
  }

  public DialogView getView() {
    if (this.photoView == null) {
      this.photoView = this.viewFactory.createPhotoView(this.home, this.preferences, this);
    }
    return this.photoView;
  }

  public void displayView(View parentView) {
    getView().displayView(parentView);
  }

  public void addPropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property.name(), listener);
  }

  public void removePropertyChangeListener(Property property, PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(property.name(), listener);
  }

  protected void updateProperties() {
    if (this.home != null) {
      super.updateProperties();
      setTime(this.home.getCamera().getTime());
      setLens(this.home.getCamera().getLens());
    }
  }
  
  public void setTime(long time) {
    if (this.time != time) {
      long oldTime = this.time;
      this.time = time;
      this.propertyChangeSupport.firePropertyChange(Property.TIME.name(), oldTime, time);
      Camera homeCamera = this.home.getCamera();
      homeCamera.removePropertyChangeListener(this.cameraChangeListener);
      homeCamera.setTime(time);
      homeCamera.addPropertyChangeListener(this.cameraChangeListener);
    }
  }
  
  public long getTime() {
    return this.time;
  }

  public void setLens(Camera.Lens lens) {
    if (this.lens != lens) {
      Camera.Lens oldLens = this.lens;
      this.lens = lens;
      this.propertyChangeSupport.firePropertyChange(Property.LENS.name(), oldLens, lens);
      if (lens == Camera.Lens.SPHERICAL) {
        setAspectRatio(AspectRatio.RATIO_2_1);
      } else if (lens == Camera.Lens.FISHEYE) {
        setAspectRatio(AspectRatio.SQUARE_RATIO);
      }  
      this.home.getCamera().setLens(this.lens);
    }
  }
  
  public Camera.Lens getLens() {    
    return this.lens;
  }
}
