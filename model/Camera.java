package com.eteks.homeview3d.model;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class Camera extends HomeObject {
  public enum Lens {PINHOLE, NORMAL, FISHEYE, SPHERICAL} 

  public enum Property {NAME, X, Y, Z, YAW, PITCH, FIELD_OF_VIEW, TIME, LENS}
  
  private static final long serialVersionUID = 1L;
  
  private String              name;
  private float               x;
  private float               y;
  private float               z;
  private float               yaw;
  private float               pitch;
  private float               fieldOfView;
  private long                time;
  private transient Lens      lens;
  private String              lensName;

  private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);

  public Camera(float x, float y, float z, float yaw, float pitch, float fieldOfView) {
    this(x, y, z, yaw, pitch, fieldOfView, midday(), Lens.PINHOLE);
  }

  /**
   * Ư�� ��ġ�� �������� ī�޶� ����
   */
  public Camera(float x, float y, float z, float yaw, float pitch, float fieldOfView, 
                long time, Lens lens) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.yaw = yaw;
    this.pitch = pitch;
    this.fieldOfView = fieldOfView;
    this.time = time;
    this.lens = lens;
  }

  private static long midday() {
    Calendar midday = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    midday.set(Calendar.HOUR_OF_DAY, 12);
    midday.set(Calendar.MINUTE, 0);
    midday.set(Calendar.SECOND, 0);
    midday.set(Calendar.MILLISECOND, 0);
    return midday.getTimeInMillis();
  }
  
  /**
   * ���ο� ī�޶� �ʵ� ���� �� �Ӽ� �б�
   */
  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    this.time = midday();
    this.lens = Lens.PINHOLE;
    in.defaultReadObject();
    try {
      if (this.lensName != null) {
        this.lens = Lens.valueOf(this.lensName);
      }
    } catch (IllegalArgumentException ex) {
    }
  }

  private void writeObject(java.io.ObjectOutputStream out) throws IOException {

    this.lensName = this.lens.name();
    out.defaultWriteObject();
  }
  
  /**
   * �Ӽ���ȭ �߰�
   */
  public void addPropertyChangeListener(PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(listener);
  }

  /**
   * �Ӽ���ȭ ����
   */
  public void removePropertyChangeListener(PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(listener);
  }

  /**
   * ī�޶� �̸� ��ȯ
   */
  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    if (name != this.name
        && (name == null || !name.equals(this.name))) {
      String oldName = this.name;
      this.name = name;
      this.propertyChangeSupport.firePropertyChange(Property.NAME.name(), oldName, name);
    }
  }

  /**
   * ��䰢 ��ȯ.
   */
  public float getYaw() {
    return this.yaw;
  }

  /**
   * ��䰢�� ���� ������ ����.
   */
  public void setYaw(float yaw) {
    if (yaw != this.yaw) {
      float oldYaw = this.yaw;
      this.yaw = yaw;
      this.propertyChangeSupport.firePropertyChange(Property.YAW.name(), oldYaw, yaw);
    }
  }
  
  /**
   * ��絵 ��ȯ.
   */
  public float getPitch() {
    return this.pitch;
  }

  /**
   * ��絵 ���� ������ ����.
   */
  public void setPitch(float pitch) {
    if (pitch != this.pitch) {
      float oldPitch = this.pitch;
      this.pitch = pitch;
      this.propertyChangeSupport.firePropertyChange(Property.PITCH.name(), oldPitch, pitch);
    }
  }

  /**
   * Returns the field of view in radians of this camera.
   */
  public float getFieldOfView() {
    return this.fieldOfView;
  }

  /**
   * �ʵ�並 ���� ������ ����.
   */
  public void setFieldOfView(float fieldOfView) {
    if (fieldOfView != this.fieldOfView) {
      float oldFieldOfView = this.fieldOfView;
      this.fieldOfView = fieldOfView;
      this.propertyChangeSupport.firePropertyChange(Property.FIELD_OF_VIEW.name(), oldFieldOfView, fieldOfView);
    }
  }

  /**
   * x�� ��ǥ ��ȯ.
   */
  public float getX() {
    return this.x;
  }

  /**
   * ī�޶� x�� ����
   */
  public void setX(float x) {
    if (x != this.x) {
      float oldX = this.x;
      this.x = x;
      this.propertyChangeSupport.firePropertyChange(Property.X.name(), oldX, x);
    }
  }
  
  /**
   * y�� ��ǥ ��ȯ.
   */
  public float getY() {
    return this.y;
  }

  /**
   * ī�޶� y�� ����.
   */
  public void setY(float y) {
    if (y != this.y) {
      float oldY = this.y;
      this.y = y;
      this.propertyChangeSupport.firePropertyChange(Property.Y.name(), oldY, y);
    }
  }
  
  /**
   * ī�޶� z�� ��ȯ.
   */
  public float getZ() {
    return this.z;
  }
  
  /**
   * ī�޶� z�� ����.
   */
  public void setZ(float z) {
    if (z != this.z) {
      float oldZ = this.z;
      this.z = z;
      this.propertyChangeSupport.firePropertyChange(Property.Z.name(), oldZ, z);
    }
  }

  public long getTime() {
    return this.time;
  }

  public void setTime(long time) {
    if (this.time != time) {
      long oldTime = this.time;
      this.time = time;
      this.propertyChangeSupport.firePropertyChange(Property.TIME.name(), 
          oldTime, time);
    }
  }

  public static long convertTimeToTimeZone(long utcTime, String timeZone) { 
    Calendar utcCalendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
    utcCalendar.setTimeInMillis(utcTime);
    Calendar convertedCalendar = new GregorianCalendar(TimeZone.getTimeZone(timeZone));
    convertedCalendar.set(Calendar.YEAR, utcCalendar.get(Calendar.YEAR));
    convertedCalendar.set(Calendar.MONTH, utcCalendar.get(Calendar.MONTH));
    convertedCalendar.set(Calendar.DAY_OF_MONTH, utcCalendar.get(Calendar.DAY_OF_MONTH));
    convertedCalendar.set(Calendar.HOUR_OF_DAY, utcCalendar.get(Calendar.HOUR_OF_DAY));
    convertedCalendar.set(Calendar.MINUTE, utcCalendar.get(Calendar.MINUTE));
    convertedCalendar.set(Calendar.SECOND, utcCalendar.get(Calendar.SECOND));
    convertedCalendar.set(Calendar.MILLISECOND, utcCalendar.get(Calendar.MILLISECOND));
    return convertedCalendar.getTimeInMillis();
  }

  public Lens getLens() {
    return this.lens;
  }
  
  public void setLens(Lens lens) {
    if (lens != this.lens) {
      Lens oldLens = this.lens;
      this.lens = lens;
      this.propertyChangeSupport.firePropertyChange(Property.LENS.name(), oldLens, lens);
    }
  }

  public void setCamera(Camera camera) {
    setX(camera.getX());
    setY(camera.getY());
    setZ(camera.getZ());
    setYaw(camera.getYaw());
    setPitch(camera.getPitch());
    setFieldOfView(camera.getFieldOfView());
  }
  
  public Camera clone() {
    Camera clone = (Camera)super.clone();
    clone.propertyChangeSupport = new PropertyChangeSupport(clone);
    return clone;
  }
}
