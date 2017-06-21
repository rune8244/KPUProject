package com.eteks.homeview3d.model;

import java.util.ArrayList;
import java.util.List;

public class CollectionChangeSupport<T> {
  private final Object                      source;
  private final List<CollectionListener<T>> collectionListeners;

  public CollectionChangeSupport(Object source) {
    this.source = source;
    this.collectionListeners = new ArrayList<CollectionListener<T>>(5);
  }

  public void addCollectionListener(CollectionListener<T> listener) {
    this.collectionListeners.add(listener);
  }

  public void removeCollectionListener(CollectionListener<T> listener) {
    this.collectionListeners.remove(listener);
  }

  public void fireCollectionChanged(T item, CollectionEvent.Type eventType) {
    fireCollectionChanged(item, -1, eventType);
  }

  @SuppressWarnings("unchecked")
  public void fireCollectionChanged(T item, int index, 
                                    CollectionEvent.Type eventType) {
    if (!this.collectionListeners.isEmpty()) {
      CollectionEvent<T> event = new CollectionEvent<T>(this.source, item, index, eventType);
      CollectionListener<T> [] listeners = this.collectionListeners.
        toArray(new CollectionListener [this.collectionListeners.size()]);
      for (CollectionListener<T> listener : listeners) {
        listener.collectionChanged(event);
      }
    }
  }
}
