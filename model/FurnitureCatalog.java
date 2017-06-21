package com.eteks.homeview3d.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FurnitureCatalog {
  private List<FurnitureCategory>       categories = new ArrayList<FurnitureCategory>();
  private final CollectionChangeSupport<CatalogPieceOfFurniture> furnitureChangeSupport = 
                             new CollectionChangeSupport<CatalogPieceOfFurniture>(this);

  public List<FurnitureCategory> getCategories() {
    return Collections.unmodifiableList(this.categories);
  }

  public int getCategoriesCount() {
    return this.categories.size();
  }

  public FurnitureCategory getCategory(int index) {
    return this.categories.get(index);
  }

  public void addFurnitureListener(CollectionListener<CatalogPieceOfFurniture> listener) {
    this.furnitureChangeSupport.addCollectionListener(listener);
  }

  public void removeFurnitureListener(CollectionListener<CatalogPieceOfFurniture> listener) {
    this.furnitureChangeSupport.removeCollectionListener(listener);
  }

  public void add(FurnitureCategory category, CatalogPieceOfFurniture piece) {
    int index = Collections.binarySearch(this.categories, category);
    // 카테고리 존재하지 않을 경우 카테고리에 추가
    if (index < 0) {
      category = new FurnitureCategory(category.getName());
      this.categories.add(-index - 1, category);
    } else {
      category = this.categories.get(index);
    }    
    // 현재 가구를 카테고리 목록에 추가
    category.add(piece);

    this.furnitureChangeSupport.fireCollectionChanged(piece, 
        category.getIndexOfPieceOfFurniture(piece), CollectionEvent.Type.ADD);
  }

  public void delete(CatalogPieceOfFurniture piece) {
    FurnitureCategory category = piece.getCategory();
    if (category != null) {
      int pieceIndex = category.getIndexOfPieceOfFurniture(piece);
      if (pieceIndex >= 0) {
        category.delete(piece);
        
        if (category.getFurnitureCount() == 0) {
          this.categories = new ArrayList<FurnitureCategory>(this.categories);
          this.categories.remove(category);
        }
        
        this.furnitureChangeSupport.fireCollectionChanged(piece, pieceIndex, CollectionEvent.Type.DELETE);
        return;
      }
    }

    throw new IllegalArgumentException("catalog doesn't contain piece " + piece.getName());
  }
}
