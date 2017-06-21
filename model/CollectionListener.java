package com.eteks.homeview3d.model;

import java.util.EventListener;

public interface CollectionListener<T> extends EventListener {
  public void collectionChanged(CollectionEvent<T> ev);
}
