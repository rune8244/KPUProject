package com.eteks.homeview3d.model;

public interface HomeRecorder {

  public enum Type {
    DEFAULT, 
    COMPRESSED}

  public void writeHome(Home home, String name) throws RecorderException;

  public Home readHome(String name) throws RecorderException;

  public boolean exists(String name) throws RecorderException;
}
