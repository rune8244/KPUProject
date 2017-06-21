package com.eteks.homeview3d.model;

import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class CatalogTexture implements TextureImage, CatalogItem, Comparable<CatalogTexture> {
  private static final long serialVersionUID = 1L;
  private static final byte [][]  EMPTY_CRITERIA     = new byte [0][];
 
  private final String          id;
  private final String          name;
  private final Content         image;
  private final float           width;
  private final float           height;
  private final String          creator;
  private final boolean         modifiable;
  
  private TexturesCategory      category;
  private byte []               filterCollationKey;
  
  private static final Collator COMPARATOR;
  private static final Map<String, byte [][]> recentFilters;
  
  static {
    COMPARATOR = Collator.getInstance();
    COMPARATOR.setStrength(Collator.PRIMARY); 
    recentFilters = new WeakHashMap<String, byte[][]>();
  }

  public CatalogTexture(String name, Content image, float width, float height) {
    this(null, name, image, width, height, null);
  }

  public CatalogTexture(String id, 
                        String name, Content image, 
                        float width, float height,
                        String creator) {
    this(id, name, image, width, height, creator, false);
  }

  public CatalogTexture(String name, Content image, 
                        float width, float height,
                        boolean modifiable) {
    this(null, name, image, width, height, null, modifiable);
  }
  
  public CatalogTexture(String id, 
                        String name, Content image, 
                        float width, float height,
                        String creator,
                        boolean modifiable) {
    this.id = id;
    this.name = name;
    this.image = image;
    this.width = width;
    this.height = height;
    this.creator = creator;
    this.modifiable = modifiable;
  }

  public String getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

  public Content getImage() {
    return this.image;
  }

  public Content getIcon() {
    return getImage();
  }

  public float getWidth() {
    return this.width;
  }

  public float getHeight() {
    return this.height;
  }

  public String getCreator() {
    return this.creator;
  }

  public boolean isModifiable() {
    return this.modifiable;
  }

  public TexturesCategory getCategory() {
    return this.category;
  }

  void setCategory(TexturesCategory category) {
    this.category = category;
  }

  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  public int hashCode() {
    return super.hashCode();
  }

  public int compareTo(CatalogTexture texture) {
    int nameComparison = COMPARATOR.compare(this.name, texture.name);
    if (nameComparison != 0) {
      return nameComparison;
    } else {
      return this.modifiable == texture.modifiable 
          ? 0
          : (this.modifiable ? 1 : -1); 
    }
  }

  public boolean matchesFilter(String filter) {
    byte [][] filterCriteriaCollationKeys = getFilterCollationKeys(filter);
    int checkedCriteria = 0;
    if (filterCriteriaCollationKeys.length > 0) {
      byte [] furnitureCollationKey = getTextureCollationKey();
      for (int i = 0; i < filterCriteriaCollationKeys.length; i++) {
        if (isSubCollationKey(furnitureCollationKey, filterCriteriaCollationKeys [i], 0)) {
          checkedCriteria++;
        } else {
          break;
        }
      }
    }
    return checkedCriteria == filterCriteriaCollationKeys.length;
  }

  private byte [][] getFilterCollationKeys(String filter) {
    if (filter.length() == 0) {
      return EMPTY_CRITERIA;
    }
    byte [][] filterCollationKeys = recentFilters.get(filter);
    if (filterCollationKeys == null) {
      String [] filterCriteria = filter.split("\\s|\\p{Punct}|\\|");
      List<byte []> filterCriteriaCollationKeys = new ArrayList<byte []>(filterCriteria.length);
      for (String criterion : filterCriteria) {
        if (criterion.length() > 0) {
          filterCriteriaCollationKeys.add(COMPARATOR.getCollationKey(criterion).toByteArray());
        }
      }
      if (filterCriteriaCollationKeys.size() == 0) {
        filterCollationKeys = EMPTY_CRITERIA;
      } else {
        filterCollationKeys = filterCriteriaCollationKeys.toArray(new byte [filterCriteriaCollationKeys.size()][]);
      }
      recentFilters.put(filter, filterCollationKeys);
    }
    return filterCollationKeys;
  }

  private byte [] getTextureCollationKey() {
    if (this.filterCollationKey == null) {
      StringBuilder search = new StringBuilder();
      search.append(getName());
      search.append('|');
      if (getCategory() != null) {
        search.append(getCategory().getName());
        search.append('|');
      }
      if (getCreator() != null) {
        search.append(getCreator());
        search.append('|');
      }
      this.filterCollationKey = COMPARATOR.getCollationKey(search.toString()).toByteArray();
    }
    return this.filterCollationKey;
  }

  private boolean isSubCollationKey(byte [] collationKey, byte [] filterCollationKey, int start) {
    for (int i = start, n = collationKey.length - 4, m = filterCollationKey.length - 4; i < n && i < n - m + 1; i++) {
      if (collationKey [i] == filterCollationKey [0]) {
        for (int j = 1; j < m; j++) {
          if (collationKey [i + j] != filterCollationKey [j]) {
            return isSubCollationKey(collationKey, filterCollationKey, i + 1);
          }
        }
        return true;
      }
    }
    return false;
  }
}
