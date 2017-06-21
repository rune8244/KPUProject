package com.eteks.homeview3d.model;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FurnitureCategory implements Comparable<FurnitureCategory> {
  private final String                  name;
  private List<CatalogPieceOfFurniture> furniture;
  
  private static final Collator  COMPARATOR = Collator.getInstance();

  public FurnitureCategory(String name) {
    this.name = name;
    this.furniture = new ArrayList<CatalogPieceOfFurniture>();
  }

  public String getName() {
    return this.name;
  }

  public List<CatalogPieceOfFurniture> getFurniture() {
    return Collections.unmodifiableList(this.furniture);
  }

  public int getFurnitureCount() {
    return this.furniture.size();
  }

  public CatalogPieceOfFurniture getPieceOfFurniture(int index) {
    return this.furniture.get(index);
  }

  public int getIndexOfPieceOfFurniture(CatalogPieceOfFurniture piece) {
    return this.furniture.indexOf(piece);
  }

  void add(CatalogPieceOfFurniture piece) {
    piece.setCategory(this);
    int index = Collections.binarySearch(this.furniture, piece);
    if (index < 0) {
      index = -index - 1;
    } 
    this.furniture.add(index, piece);    
  }

  void delete(CatalogPieceOfFurniture piece) {
    int pieceIndex = this.furniture.indexOf(piece);
    if (pieceIndex == -1) {
      throw new IllegalArgumentException(
          this.name + " doesn't contain piece " + piece.getName());
    }
    this.furniture = new ArrayList<CatalogPieceOfFurniture>(this.furniture);
    this.furniture.remove(pieceIndex);
  }

  public boolean equals(Object obj) {
    return obj instanceof FurnitureCategory
           && COMPARATOR.equals(this.name, ((FurnitureCategory)obj).name);
  }

  public int hashCode() {
    return this.name.hashCode();
  }

  public int compareTo(FurnitureCategory category) {
    return COMPARATOR.compare(this.name, category.name);
  }
}
