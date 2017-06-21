package com.eteks.homeview3d.viewcontroller;

import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.Selectable;

public interface Object3DFactory {  
  public abstract Object createObject3D(Home home, Selectable item, boolean waitForLoading);
}
