package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;

import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomeEnvironment;
import com.eteks.homeview3d.model.Level;
import com.eteks.homeview3d.model.ObserverCamera;
import com.eteks.homeview3d.model.UserPreferences;

public class ObserverCameraController implements Controller {
  public enum Property {X, Y, ELEVATION, MINIMUM_ELEVATION,
      YAW_IN_DEGREES, PITCH_IN_DEGREES, FIELD_OF_VIEW_IN_DEGREES, 
      OBSERVER_CAMERA_ELEVATION_ADJUSTED}
  
  private final Home                  home;
  private final UserPreferences       preferences;
  private final ViewFactory           viewFactory;
  private final PropertyChangeSupport propertyChangeSupport;
  private DialogView                  observerCameraView;

  private float             x;
  private float             y;
  private float             elevation;
  private float             minimumElevation;
  private int               yawInDegrees;
  private int               pitchInDegrees;
  private int               fieldOfViewInDegrees;
  private boolean           elevationAdjusted;

  public ObserverCameraController(Home home,
                                  UserPreferences preferences,
                                  ViewFactory viewFactory) {
    this.home = home;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    
    updateProperties();
  }

  public DialogView getView() {
    if (this.observerCameraView == null) {
      this.observerCameraView = this.viewFactory.createObserverCameraView(this.preferences, this); 
    }
    return this.observerCameraView;
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
    ObserverCamera observerCamera = this.home.getObserverCamera();
    setX(observerCamera.getX());
    setY(observerCamera.getY());
    List<Level> levels = this.home.getLevels();
    setMinimumElevation(levels.size() == 0 
        ? 10  
        : 10 + levels.get(0).getElevation());
    setElevation(observerCamera.getZ());
    setYawInDegrees((int)(Math.round(Math.toDegrees(observerCamera.getYaw()))));
    setPitchInDegrees((int)(Math.round(Math.toDegrees(observerCamera.getPitch()))));
    setFieldOfViewInDegrees((int)(Math.round(Math.toDegrees(
        observerCamera.getFieldOfView())) + 360) % 360);
    HomeEnvironment homeEnvironment = this.home.getEnvironment();
    setElevationAdjusted(homeEnvironment.isObserverCameraElevationAdjusted());
  }

  public void setX(float x) {
    if (x != this.x) {
      float oldX = this.x;
      this.x = x;
      this.propertyChangeSupport.firePropertyChange(Property.X.name(), oldX, x);
    }
  }

  public float getX() {
    return this.x;
  }

  public void setY(float y) {
    if (y != this.y) {
      float oldY = this.y;
      this.y = y;
      this.propertyChangeSupport.firePropertyChange(Property.Y.name(), oldY, y);
    }
  }

  public float getY() {
    return this.y;
  }

  public void setElevation(float elevation) {
    if (elevation != this.elevation) {
      float oldObserverCameraElevation = this.elevation;
      this.elevation = elevation;
      this.propertyChangeSupport.firePropertyChange(Property.ELEVATION.name(), oldObserverCameraElevation, elevation);
    }
  }

  public float getElevation() {
    return this.elevation;
  }

  private void setMinimumElevation(float minimumElevation) {
    if (minimumElevation != this.minimumElevation) {
      float oldMinimumElevation = this.minimumElevation;
      this.minimumElevation = minimumElevation;
      this.propertyChangeSupport.firePropertyChange(Property.MINIMUM_ELEVATION.name(), oldMinimumElevation, minimumElevation);
    }
  }

  public float getMinimumElevation() {
    return this.minimumElevation;
  }

  public boolean isElevationAdjusted() {
    return this.elevationAdjusted;
  }
  
  public void setElevationAdjusted(boolean observerCameraElevationAdjusted) {
    if (this.elevationAdjusted != observerCameraElevationAdjusted) {
      this.elevationAdjusted = observerCameraElevationAdjusted;
      this.propertyChangeSupport.firePropertyChange(Property.OBSERVER_CAMERA_ELEVATION_ADJUSTED.name(), 
          !observerCameraElevationAdjusted, observerCameraElevationAdjusted);
      Level selectedLevel = this.home.getSelectedLevel();
      if (selectedLevel != null) {
        if (observerCameraElevationAdjusted) {
          setElevation(getElevation() - selectedLevel.getElevation());
        } else {
          setElevation(getElevation() + selectedLevel.getElevation());
        }
      }
    }
  }
 
  public boolean isObserverCameraElevationAdjustedEditable() {
    return this.home.getLevels().size() > 1;
  }
  
  public void setYawInDegrees(int yawInDegrees) {
    if (yawInDegrees != this.yawInDegrees) {
      int oldYawInDegrees = this.yawInDegrees;
      this.yawInDegrees = yawInDegrees;
      this.propertyChangeSupport.firePropertyChange(Property.YAW_IN_DEGREES.name(), oldYawInDegrees, yawInDegrees);
    }
  }

  public int getYawInDegrees() {
    return this.yawInDegrees;
  }

  public void setPitchInDegrees(int pitchInDegrees) {
    if (pitchInDegrees != this.pitchInDegrees) {
      int oldPitchInDegrees = this.pitchInDegrees;
      this.pitchInDegrees = pitchInDegrees;
      this.propertyChangeSupport.firePropertyChange(Property.PITCH_IN_DEGREES.name(), oldPitchInDegrees, pitchInDegrees);
    }
  }

  public int getPitchInDegrees() {
    return this.pitchInDegrees;
  }

  public void setFieldOfViewInDegrees(int observerFieldOfViewInDegrees) {
    if (observerFieldOfViewInDegrees != this.fieldOfViewInDegrees) {
      int oldObserverFieldOfViewInDegrees = this.fieldOfViewInDegrees;
      this.fieldOfViewInDegrees = observerFieldOfViewInDegrees;
      this.propertyChangeSupport.firePropertyChange(Property.FIELD_OF_VIEW_IN_DEGREES.name(), 
          oldObserverFieldOfViewInDegrees, observerFieldOfViewInDegrees);
    }
  }

  public int getFieldOfViewInDegrees() {
    return this.fieldOfViewInDegrees;
  }

  public void modifyObserverCamera() {
    float x = getX();
    float y = getY();
    float z = getElevation();
    boolean observerCameraElevationAdjusted = isElevationAdjusted();
    Level selectedLevel = this.home.getSelectedLevel();
    if (observerCameraElevationAdjusted && selectedLevel != null) {
      z += selectedLevel.getElevation();
      List<Level> levels = this.home.getLevels();
      z = Math.max(z, levels.size() == 0  ? 10  : 10 + levels.get(0).getElevation());
    }
    float yaw = (float)Math.toRadians(getYawInDegrees());
    float pitch = (float)Math.toRadians(getPitchInDegrees());
    float fieldOfView = (float)Math.toRadians(getFieldOfViewInDegrees());
    ObserverCamera observerCamera = this.home.getObserverCamera();
    observerCamera.setX(x);
    observerCamera.setY(y);
    observerCamera.setZ(z);
    observerCamera.setYaw(yaw);
    observerCamera.setPitch(pitch);
    observerCamera.setFieldOfView(fieldOfView);
    HomeEnvironment homeEnvironment = this.home.getEnvironment();
    homeEnvironment.setObserverCameraElevationAdjusted(observerCameraElevationAdjusted);
  }
}
