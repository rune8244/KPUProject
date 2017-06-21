package com.eteks.homeview3d.viewcontroller;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import javax.swing.undo.UndoableEditSupport;

import com.eteks.homeview3d.model.Compass;
import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.UserPreferences;


public class CompassController implements Controller {  
  public enum Property {X, Y, DIAMETER, VISIBLE, NORTH_DIRECTION_IN_DEGREES, 
      LATITUDE_IN_DEGREES, LONGITUDE_IN_DEGREES, TIME_ZONE}
  
  private final Home                  home;
  private final UserPreferences       preferences;
  private final ViewFactory           viewFactory;
  private final UndoableEditSupport   undoSupport;
  private final PropertyChangeSupport propertyChangeSupport;
  private DialogView                  compassView;
  
  private float   x;
  private float   y;
  private float   diameter;
  private boolean visible;
  private float   northDirectionInDegrees;
  private float   latitudeInDegrees;
  private float   longitudeInDegrees;
  private String  timeZone;

  public CompassController(Home home, 
                           UserPreferences preferences, 
                           ViewFactory viewFactory,
                           UndoableEditSupport undoSupport) {
    this.home = home;
    this.preferences = preferences;
    this.viewFactory = viewFactory;
    this.undoSupport = undoSupport;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    
    updateProperties();
  }

  public DialogView getView() {
    if (this.compassView == null) {
      this.compassView = this.viewFactory.createCompassView(
          this.preferences, this); 
    }
    return this.compassView;
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
    Compass compass = this.home.getCompass();
    setX(compass.getX());
    setY(compass.getY());
    setDiameter(compass.getDiameter());
    setVisible(compass.isVisible());
    setNorthDirectionInDegrees((float)Math.toDegrees(compass.getNorthDirection()));
    setLatitudeInDegrees((float)Math.toDegrees(compass.getLatitude()));
    setLongitudeInDegrees((float)Math.toDegrees(compass.getLongitude()));
    setTimeZone(compass.getTimeZone());
  }  

 
  public float getX() {
    return this.x;
  }
  
 
  public void setX(float x) {
    if (x != this.x) {
      float oldX = this.x;
      this.x = x;
      this.propertyChangeSupport.firePropertyChange(Property.X.name(), oldX, x);
    }
  }
  
 
  public float getY() {
    return this.y;
  }
  
  public void setY(float y) {
    if (y != this.y) {
      float oldY = this.y;
      this.y = y;
      this.propertyChangeSupport.firePropertyChange(Property.Y.name(), oldY, y);
    }
  }

  
  public float getDiameter() {
    return this.diameter;
  }
  
 
  public void setDiameter(float diameter) {
    if (diameter != this.diameter) {
      float oldDiameter = this.diameter;
      this.diameter = diameter;
      this.propertyChangeSupport.firePropertyChange(Property.DIAMETER.name(), oldDiameter, diameter);
    }
  }

  
  public boolean isVisible() {
    return this.visible;
  }
  
  
  public void setVisible(boolean visible) {
    if (visible != this.visible) {
      this.visible = visible;
      this.propertyChangeSupport.firePropertyChange(Property.VISIBLE.name(), !visible, visible);
    }
  }

  
  public float getNorthDirectionInDegrees() {
    return this.northDirectionInDegrees;
  }
  
  public void setNorthDirectionInDegrees(float northDirectionInDegrees) {
    if (northDirectionInDegrees != this.northDirectionInDegrees) {
      float oldNorthDirectionInDegrees = this.northDirectionInDegrees;
      this.northDirectionInDegrees = northDirectionInDegrees;
      this.propertyChangeSupport.firePropertyChange(Property.NORTH_DIRECTION_IN_DEGREES.name(), 
          oldNorthDirectionInDegrees, northDirectionInDegrees);
    }
  }
  
 
  public final float getLatitudeInDegrees() {
    return this.latitudeInDegrees;
  }
  
  
  public void setLatitudeInDegrees(float latitudeInDegrees) {
    if (latitudeInDegrees != this.latitudeInDegrees) {
      float oldLatitudeInDegrees = this.latitudeInDegrees;
      this.latitudeInDegrees = latitudeInDegrees;
      this.propertyChangeSupport.firePropertyChange(Property.LATITUDE_IN_DEGREES.name(), oldLatitudeInDegrees, latitudeInDegrees);
    }
  }
 
  public final float getLongitudeInDegrees() {
    return this.longitudeInDegrees;
  }

  public void setLongitudeInDegrees(float longitudeInDegrees) {
    if (longitudeInDegrees != this.longitudeInDegrees) {
      float oldLongitudeInDegrees = this.longitudeInDegrees;
      this.longitudeInDegrees = longitudeInDegrees;
      this.propertyChangeSupport.firePropertyChange(Property.LONGITUDE_IN_DEGREES.name(), oldLongitudeInDegrees, longitudeInDegrees);
    }
  }

  public String getTimeZone() {
    return this.timeZone;
  }

  public void setTimeZone(String timeZone) {
    if (!timeZone.equals(this.timeZone)) {
      String oldTimeZone = this.timeZone;
      this.timeZone = timeZone;
      this.propertyChangeSupport.firePropertyChange(Property.TIME_ZONE.name(), oldTimeZone, timeZone);
    }
  }

  public void modifyCompass() {
    float x = getX();
    float y = getY();
    float diameter = getDiameter();
    boolean visible = isVisible();
    float northDirection = (float)Math.toRadians(getNorthDirectionInDegrees());
    float latitude = (float)Math.toRadians(getLatitudeInDegrees());
    float longitude = (float)Math.toRadians(getLongitudeInDegrees());
    String timeZone = getTimeZone();
    UndoableEdit undoableEdit = 
        new CompassUndoableEdit(this.home.getCompass(), this.preferences, 
            x, y, diameter, visible, northDirection, latitude, longitude, timeZone);
    doModifyCompass(this.home.getCompass(), x, y, diameter, visible, northDirection, latitude, longitude, timeZone);
    this.undoSupport.postEdit(undoableEdit);
  }
  
  private static class CompassUndoableEdit extends AbstractUndoableEdit {
    private final Compass compass;
    private final UserPreferences preferences;
    private final float oldX;
    private final float oldY;
    private final float oldDiameter;
    private final float oldNorthDirection;
    private final float oldLatitude;
    private final float oldLongitude;
    private final String oldTimeZone;
    private final boolean oldVisible;
    private final float newX;
    private final float newY;
    private final float newDiameter;
    private final float newNorthDirection;
    private final float newLatitude;
    private final float newLongitude;
    private final String newTimeZone;
    private final boolean newVisible;
  
    public CompassUndoableEdit(Compass compass, UserPreferences preferences, float newX, float newY,
                               float newDiameter, boolean newVisible, float newNorthDirection, 
                               float newLatitude, float newLongitude, String newTimeZone) {
      this.compass = compass;
      this.preferences = preferences;
      this.oldX = compass.getX();
      this.oldY = compass.getY();
      this.oldDiameter = compass.getDiameter();
      this.oldVisible = compass.isVisible();
      this.oldNorthDirection = compass.getNorthDirection();
      this.oldLatitude = compass.getLatitude();
      this.oldLongitude = compass.getLongitude();
      this.oldTimeZone = compass.getTimeZone();
      this.newX = newX;
      this.newY = newY;
      this.newDiameter = newDiameter;
      this.newVisible = newVisible;
      this.newNorthDirection = newNorthDirection;
      this.newLatitude = newLatitude;
      this.newLongitude = newLongitude;
      this.newTimeZone = newTimeZone;
    }

    @Override
    public void undo() throws CannotUndoException {
      super.undo();
      doModifyCompass(this.compass, this.oldX, this.oldY, this.oldDiameter, this.oldVisible, 
          this.oldNorthDirection, this.oldLatitude, this.oldLongitude, this.oldTimeZone);
    }
  
    @Override
    public void redo() throws CannotRedoException {
      super.redo();
      doModifyCompass(this.compass, this.newX, this.newY, this.newDiameter, this.newVisible, 
          this.newNorthDirection, this.newLatitude, this.newLongitude, this.newTimeZone);
    }
  
    @Override
    public String getPresentationName() {
      return this.preferences.getLocalizedString(CompassController.class, "undoModifyCompassName");
    }
  }
  
  private static void doModifyCompass(Compass compass, float x, float y, float diameter, boolean visible, 
                                      float northDirection, float latitude, float longitude, String timeZone) {
    compass.setX(x);
    compass.setY(y);
    compass.setDiameter(diameter);
    compass.setVisible(visible);
    compass.setNorthDirection(northDirection);
    compass.setLatitude(latitude);
    compass.setLongitude(longitude);
    compass.setTimeZone(timeZone);
  }
}
