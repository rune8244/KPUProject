package com.eteks.homeview3d.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TexturesCatalog {
  private List<TexturesCategory>  categories = new ArrayList<TexturesCategory>();
  private final CollectionChangeSupport<CatalogTexture> texturesChangeSupport = 
                                      new CollectionChangeSupport<CatalogTexture>(this);

  public List<TexturesCategory> getCategories() {
    return Collections.unmodifiableList(this.categories);
  }

  public int getCategoriesCount() {
    return this.categories.size();
  }

  public TexturesCategory getCategory(int index) {
    return this.categories.get(index);
  }

  public void addTexturesListener(CollectionListener<CatalogTexture> listener) {
    this.texturesChangeSupport.addCollectionListener(listener);
  }

  public void removeTexturesListener(CollectionListener<CatalogTexture> listener) {
    this.texturesChangeSupport.removeCollectionListener(listener);
  }

  public void add(TexturesCategory category, CatalogTexture texture) {
    int index = Collections.binarySearch(this.categories, category);
    if (index < 0) {
      category = new TexturesCategory(category.getName());
      this.categories.add(-index - 1, category);
    } else {
      category = this.categories.get(index);
    }    
    category.add(texture);
    
    this.texturesChangeSupport.fireCollectionChanged(texture, 
        category.getIndexOfTexture(texture), CollectionEvent.Type.ADD);
  }

  public void delete(CatalogTexture texture) {
    TexturesCategory category = texture.getCategory();
    if (category != null) {
      int textureIndex = category.getIndexOfTexture(texture);
      if (textureIndex >= 0) {
        category.delete(texture);
        
        if (category.getTexturesCount() == 0) {
          this.categories = new ArrayList<TexturesCategory>(this.categories);
          this.categories.remove(category);
        }
        
        this.texturesChangeSupport.fireCollectionChanged(texture, textureIndex, CollectionEvent.Type.DELETE);
        return;
      }
    }

    throw new IllegalArgumentException("catalog doesn't contain texture " + texture.getName());
  }
}
