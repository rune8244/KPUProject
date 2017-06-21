package com.eteks.homeview3d.model;

import java.util.EventListener;

public interface SelectionListener extends EventListener {

  void selectionChanged(SelectionEvent selectionEvent);
}
