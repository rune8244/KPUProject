package com.eteks.homeview3d.j3d;

import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.HomePieceOfFurniture;
import com.eteks.homeview3d.model.Label;
import com.eteks.homeview3d.model.Room;
import com.eteks.homeview3d.model.Selectable;
import com.eteks.homeview3d.model.Wall;
import com.eteks.homeview3d.viewcontroller.Object3DFactory;

public class Object3DBranchFactory implements Object3DFactory {
  public Object createObject3D(Home home, Selectable item, boolean waitForLoading) {
    if (item instanceof HomePieceOfFurniture) {
      return new HomePieceOfFurniture3D((HomePieceOfFurniture)item, home, true, waitForLoading);
    } else if (item instanceof Wall) {
      return new Wall3D((Wall)item, home, true, waitForLoading);
    } else if (item instanceof Room) {
      return new Room3D((Room)item, home, false, waitForLoading);
    } else if (item instanceof Label) {
      return new Label3D((Label)item, home, waitForLoading);
    } else {
      return null;
    }  
  }
}
