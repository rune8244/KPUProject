package com.eteks.homeview3d.swing;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.List;

import com.eteks.homeview3d.model.Home;
import com.eteks.homeview3d.model.Selectable;

public class HomeTransferableList implements Transferable {
  public final static DataFlavor HOME_FLAVOR;
  
  static {
    try {
      // 리스트 생성
      String homeFlavorMimeType = 
        DataFlavor.javaJVMLocalObjectMimeType
        + ";class=" + HomeTransferableList.class.getName();
      HOME_FLAVOR = new DataFlavor(homeFlavorMimeType);
    } catch (ClassNotFoundException ex) {
      throw new RuntimeException(ex);
    }
  }
  
  // 전송된 아이템 저장
  private final List<Selectable> transferedItems;

  public HomeTransferableList(List<? extends Selectable> items) {
    this.transferedItems = Home.duplicate(items);
  }

  public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
    if (flavor.equals(HOME_FLAVOR)) {
      return Home.duplicate(this.transferedItems);
    } else {
      throw new UnsupportedFlavorException(flavor);
    }
  }

  public DataFlavor [] getTransferDataFlavors() {
    return new DataFlavor [] {HOME_FLAVOR};
  }
  
  public boolean isDataFlavorSupported(DataFlavor flavor) {
    return HOME_FLAVOR.equals(flavor);
  }
}