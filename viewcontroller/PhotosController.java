package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.eteks.homeview3d.model.Camera;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.UserPreferences;

public class PhotosController extends AbstractPhotoController { 
  public enum Property {CAMERAS, SELECTED_CAMERAS, FILE_FORMAT, FILE_COMPRESSION_QUALITY}
  
  private final Home                  home;
  private final UserPreferences       preferences;
  private final ViewFactory           viewFactory;
  private final PropertyChangeSupport propertyChangeSupport;
  private DialogView                  photoView;
  
  private List<Camera> cameras;
  private List<Camera> selectedCameras;
  private String       fileFormat;
  private Float        fileCompressionQuality;

  public PhotosController(Home home, UserPreferences preferences, View view3D, 
                          ViewFactory viewFactory, ContentManager contentManager) {
    super(home, preferences, view3D, contentManager);
    this.home = home;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    
    this.cameras = Collections.emptyList();
    this.selectedCameras = Collections.emptyList();
    
    home.addPropertyChangeListener(Home.Property.STORED_CAMERAS, new HomeStoredCamerasChangeListener(this));
    updateProperties();
  }

  private static class HomeStoredCamerasChangeListener implements PropertyChangeListener {
    private WeakReference<PhotosController> photosController;
    
    public HomeStoredCamerasChangeListener(PhotosController photoController) {
      this.photosController = new WeakReference<PhotosController>(photoController);
    }
    
    public void propertyChange(PropertyChangeEvent ev) {
      final AbstractPhotoController controller = this.photosController.get();
      if (controller == null) {
        ((Home)ev.getSource()).removePropertyChangeListener(Home.Property.STORED_CAMERAS, this);
      } else {
        controller.updateProperties();
      }
    }
  }

  public DialogView getView() {
    if (this.photoView == null) {
      this.photoView = this.viewFactory.createPhotosView(this.home, this.preferences, this);
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
      setCameras(this.home.getStoredCameras());
      setSelectedCameras(this.home.getStoredCameras());
    }    
  }

  public List<Camera> getCameras() {
    return this.cameras;
  }

  private void setCameras(List<Camera> cameras) {
    if (!cameras.equals(this.cameras)) {
      List<Camera> oldCameras = this.cameras;
      this.cameras = new ArrayList<Camera>(cameras);
      this.propertyChangeSupport.firePropertyChange(
          Property.CAMERAS.name(), Collections.unmodifiableList(oldCameras), Collections.unmodifiableList(cameras));
    }
  }

  public List<Camera> getSelectedCameras() {
    return this.selectedCameras;
  }

  public void setSelectedCameras(List<Camera> selectedCameras) {
    if (!selectedCameras.equals(this.selectedCameras)) {
      List<Camera> oldSelectedCameras = this.selectedCameras;
      this.selectedCameras = new ArrayList<Camera>(selectedCameras);
      this.propertyChangeSupport.firePropertyChange(
          Property.SELECTED_CAMERAS.name(), Collections.unmodifiableList(oldSelectedCameras), Collections.unmodifiableList(selectedCameras));
    }
  }

  public String getFileFormat() {
    return this.fileFormat;
  }

  public void setFileFormat(String fileFormat) {
    if (fileFormat != this.fileFormat) {
      String oldFileFormat = this.fileFormat;
      this.fileFormat = fileFormat;
      this.propertyChangeSupport.firePropertyChange(Property.FILE_FORMAT.name(), oldFileFormat, fileFormat);
    }
  }
  
  public Float getFileCompressionQuality() {
    return this.fileCompressionQuality;
  }

  public void setFileCompressionQuality(Float fileCompressionQuality) {
    if (fileCompressionQuality != this.fileCompressionQuality) {
      Float oldFileCompressionQuality = this.fileCompressionQuality;
      this.fileCompressionQuality = fileCompressionQuality;
      this.propertyChangeSupport.firePropertyChange(Property.FILE_COMPRESSION_QUALITY.name(), oldFileCompressionQuality, fileCompressionQuality);
    }
  }
}
