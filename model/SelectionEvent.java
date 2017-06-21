package com.eteks.homeview3d.model;

import java.util.EventObject;
import java.util.List;

public class SelectionEvent extends EventObject {
  private List<? extends Object> selectedItems;

  public SelectionEvent(Object source, List<? extends Object> selectedItems) {
    super(source);
    this.selectedItems = selectedItems;
  }

  public List<? extends Object> getSelectedItems() {
    return this.selectedItems;
  }
}
