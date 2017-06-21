package com.eteks.homeview3d.model;

import java.util.EventObject;

public class CollectionEvent<T> extends EventObject {

  public enum Type {ADD, DELETE}

  private final T    item;
  private final int  index;
  private final Type type;

  public CollectionEvent(Object source, T item, Type type) {
    this(source, item, -1, type);
  }

  public CollectionEvent(Object source, T item, int index, Type type) {
    super(source);
    this.item = item;
    this.index = index;
    this.type =  type;
  }

  public T getItem() {
    return this.item;
  }

  public int getIndex() {
    return this.index;
  }

  public Type getType() {
    return this.type;
  }
}
