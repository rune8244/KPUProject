package com.eteks.homeview3d.viewcontroller;

import com.eteks.homeview3d.model.Content;
import com.eteks.homeview3d.model.RecorderException;

public interface ContentManager {
  public enum ContentType {SWEET_HOME_3D, MODEL, IMAGE, CSV, SVG, OBJ, PNG, JPEG, MOV, PDF, LANGUAGE_LIBRARY, TEXTURES_LIBRARY, FURNITURE_LIBRARY, PLUGIN, PHOTOS_DIRECTORY, USER_DEFINED};

 
  public abstract Content getContent(String contentLocation) throws RecorderException;

  public abstract String getPresentationName(String contentLocation,
                                             ContentType contentType);

  
  public abstract boolean isAcceptable(String contentLocation,
                                       ContentType contentType);

  
  public abstract String showOpenDialog(View parentView,
                                        String dialogTitle,
                                        ContentType contentType);

 
  public abstract String showSaveDialog(View parentView,
                                        String dialogTitle,
                                        ContentType contentType,
                                        String location);
}