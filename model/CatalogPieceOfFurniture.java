package com.eteks.homeview3d.model;

import java.math.BigDecimal;
import java.text.Collator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 가구 카탈로그
 */
public class CatalogPieceOfFurniture implements Comparable<CatalogPieceOfFurniture>, PieceOfFurniture, CatalogItem {
  private static final float [][] INDENTITY_ROTATION = new float [][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
  private static final byte [][]  EMPTY_CRITERIA     = new byte [0][];

  private final String            id;
  private final String            name;
  private final String            description;
  private final String            information;
  private final String []         tags;
  private final Long              creationDate;
  private final Float             grade;
  private final Content           icon;
  private final Content           planIcon;
  private final Content           model;
  private final float             width;
  private final float             depth;
  private final float             height;
  private final boolean           proportional;
  private final float             elevation;
  private final float             dropOnTopElevation;
  private final boolean           movable;
  private final boolean           doorOrWindow;
  private final float [][]        modelRotation;
  private final String            staircaseCutOutShape;
  private final String            creator;
  private final boolean           backFaceShown;
  private final Integer           color;
  private final float             iconYaw;
  private final boolean           modifiable;
  private final boolean           resizable;
  private final boolean           deformable;
  private final boolean           texturable;
  private final BigDecimal        price;
  private final BigDecimal        valueAddedTaxPercentage;
  private final String            currency;

  private FurnitureCategory       category;
  private byte []                 filterCollationKey;

  private static final Collator               COMPARATOR;
  private static final Map<String, byte [][]> recentFilters;
  
  static {
    COMPARATOR = Collator.getInstance();
    COMPARATOR.setStrength(Collator.PRIMARY); 
    recentFilters = new WeakHashMap<String, byte[][]>();
  }

  /**
   * 가구 카탈로그 생성
   */
  public CatalogPieceOfFurniture(String name, Content icon, Content model, 
                                 float width, float depth, float height, 
                                 boolean movable, boolean doorOrWindow) {
    this(null, name, null, icon, model, width, depth, height, 0, movable, doorOrWindow, 
        INDENTITY_ROTATION, null, true, null, null);
  }

  public CatalogPieceOfFurniture(String id, String name, String description, Content icon, Content model, 
                                 float width, float depth, float height, float elevation, 
                                 boolean movable, boolean doorOrWindow, 
                                 float [][] modelRotation, String creator,
                                 boolean resizable, BigDecimal price, BigDecimal valueAddedTaxPercentage) {
    this(id, name, description, icon, model, width, depth, height, elevation, movable, 
        modelRotation, creator, resizable, price, valueAddedTaxPercentage);
  }
         
  public CatalogPieceOfFurniture(String id, String name, String description, Content icon, Content model, 
                                 float width, float depth, float height, float elevation, 
                                 boolean movable, float [][] modelRotation, String creator,
                                 boolean resizable, BigDecimal price, BigDecimal valueAddedTaxPercentage) {
    this(id, name, description, icon, null, model, width, depth, height, elevation, movable, 
        modelRotation, creator, resizable, price, valueAddedTaxPercentage);
  }

  public CatalogPieceOfFurniture(String id, String name, String description, 
                                 Content icon, Content planIcon, Content model, 
                                 float width, float depth, float height, float elevation, 
                                 boolean movable, float [][] modelRotation, String creator,
                                 boolean resizable, BigDecimal price, BigDecimal valueAddedTaxPercentage) {
    this(id, name, description, icon, planIcon, model, width, depth, height, elevation, movable, 
        modelRotation, creator, resizable, true, true, price, valueAddedTaxPercentage);
  }

  public CatalogPieceOfFurniture(String id, String name, String description, 
                                 Content icon, Content planIcon, Content model, 
                                 float width, float depth, float height, float elevation, 
                                 boolean movable, float [][] modelRotation, String creator,
                                 boolean resizable, boolean deformable, boolean texturable, 
                                 BigDecimal price, BigDecimal valueAddedTaxPercentage) {
    this(id, name, description, icon, planIcon, model, width, depth, height, elevation, 
        movable, null, modelRotation, creator, resizable, deformable, texturable,
        price, valueAddedTaxPercentage, null);
  }

  public CatalogPieceOfFurniture(String id, String name, String description, 
                                 Content icon, Content planIcon, Content model, 
                                 float width, float depth, float height, 
                                 float elevation, boolean movable, String staircaseCutOutShape, 
                                 float [][] modelRotation, String creator,
                                 boolean resizable, boolean deformable, boolean texturable, 
                                 BigDecimal price, BigDecimal valueAddedTaxPercentage, String currency) {
    this(id, name, description, null, new String [0], null, null, icon, planIcon, model, width, depth, 
        height, elevation, movable, staircaseCutOutShape, modelRotation, creator, resizable, deformable,
        texturable, price, valueAddedTaxPercentage, currency);
  }

  public CatalogPieceOfFurniture(String id, String name, String description, 
                                 String information, String [] tags, Long creationDate, Float grade, 
                                 Content icon, Content planIcon, Content model, 
                                 float width, float depth, float height, 
                                 float elevation, boolean movable, String staircaseCutOutShape, 
                                 float [][] modelRotation, String creator, 
                                 boolean resizable, boolean deformable, boolean texturable, 
                                 BigDecimal price, BigDecimal valueAddedTaxPercentage, String currency) {
    this(id, name, description, information, tags, creationDate, grade, icon, planIcon, model, width, depth, 
        height, elevation, 1f, movable, false, staircaseCutOutShape, null, modelRotation, false, creator, resizable, deformable,
        texturable, price, valueAddedTaxPercentage, currency, (float)Math.PI / 8, true, false);
  }
 
  public CatalogPieceOfFurniture(String id, String name, String description, 
                                 String information, String [] tags, Long creationDate, Float grade, 
                                 Content icon, Content planIcon, Content model, 
                                 float width, float depth, float height, 
                                 float elevation, float dropOnTopElevation, 
                                 boolean movable, String staircaseCutOutShape, 
                                 float [][] modelRotation, String creator, 
                                 boolean resizable, boolean deformable, boolean texturable, 
                                 BigDecimal price, BigDecimal valueAddedTaxPercentage, String currency) {
    this(id, name, description, information, tags, creationDate, grade, icon, planIcon, model, width, depth, height, 
        elevation, dropOnTopElevation, movable, staircaseCutOutShape, modelRotation, false, 
        creator, resizable, deformable, texturable, price, valueAddedTaxPercentage, currency);
  }
  
  public CatalogPieceOfFurniture(String id, String name, String description, 
                                 String information, String [] tags, Long creationDate, Float grade, 
                                 Content icon, Content planIcon, Content model, 
                                 float width, float depth, float height, 
                                 float elevation, float dropOnTopElevation, 
                                 boolean movable, String staircaseCutOutShape, 
                                 float [][] modelRotation, boolean backFaceShown, String creator, 
                                 boolean resizable, boolean deformable, boolean texturable, 
                                 BigDecimal price, BigDecimal valueAddedTaxPercentage, String currency) {
    this(id, name, description, information, tags, creationDate, grade, icon, planIcon, model, width, depth, 
        height, elevation, dropOnTopElevation, movable, false, staircaseCutOutShape, null, modelRotation, backFaceShown, 
        creator, resizable, deformable, texturable, price, valueAddedTaxPercentage, currency, (float)Math.PI / 8, true, false);
  }

  public CatalogPieceOfFurniture(String name, Content icon, Content model, 
                                 float width, float depth, float height, float elevation, 
                                 boolean movable, boolean doorOrWindow, Integer color,
                                 float [][] modelRotation, boolean backFaceShown,
                                 float iconYaw, boolean proportional) {
    this(name, icon, model, width, depth, height, elevation, movable, 
        color, modelRotation, backFaceShown, iconYaw, proportional);
  }

  public CatalogPieceOfFurniture(String name, Content icon, Content model, 
                                 float width, float depth, float height, float elevation, 
                                 boolean movable, Integer color,
                                 float [][] modelRotation, boolean backFaceShown,
                                 float iconYaw, boolean proportional) {
    this(name, icon, model, width, depth, height, elevation, movable,  
        null, color, modelRotation, backFaceShown, iconYaw, proportional);
  }

  public CatalogPieceOfFurniture(String name, Content icon, Content model, 
                                 float width, float depth, float height, float elevation, 
                                 boolean movable, String staircaseCutOutShape,
                                 Integer color, float [][] modelRotation, 
                                 boolean backFaceShown, float iconYaw, boolean proportional) {
    this(null, name, null, null, new String [0], System.currentTimeMillis(), null, icon, null, model, width, depth, height, elevation, 1f,
        movable, false, staircaseCutOutShape, color, modelRotation, backFaceShown, null, true, true, true, null, null, null, iconYaw, proportional, true);
  }
  
  private CatalogPieceOfFurniture(String id, String name, String description, 
                                  String information, String [] tags, Long creationDate, Float grade, 
                                  Content icon, Content planIcon, Content model, 
                                  float width, float depth, float height, 
                                  float elevation, float dropOnTopElevation, 
                                  boolean movable, boolean doorOrWindow, String staircaseCutOutShape,
                                  Integer color, float [][] modelRotation, boolean backFaceShown,
                                  String creator, boolean resizable, boolean deformable, boolean texturable, 
                                  BigDecimal price, BigDecimal valueAddedTaxPercentage, String currency, 
                                  float iconYaw, boolean proportional, boolean modifiable) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.information = information;
    this.tags = tags;
    this.creationDate = creationDate;
    this.grade = grade;
    this.icon = icon;
    this.planIcon = planIcon;
    this.model = model;
    this.width = width;
    this.depth = depth;
    this.height = height;
    this.elevation = elevation;
    this.dropOnTopElevation = dropOnTopElevation;
    this.movable = movable;
    this.doorOrWindow = doorOrWindow;
    this.color = color;
    this.staircaseCutOutShape = staircaseCutOutShape;
    this.creator = creator;
    this.price = price;
    this.valueAddedTaxPercentage = valueAddedTaxPercentage;
    this.currency = currency;
    if (modelRotation == null) {
      this.modelRotation = INDENTITY_ROTATION;
    } else {
      this.modelRotation = deepCopy(modelRotation);
    }
    this.backFaceShown = backFaceShown;
    this.resizable = resizable;
    this.deformable = deformable;
    this.texturable = texturable;
    this.iconYaw = iconYaw;
    this.proportional = proportional;
    this.modifiable = modifiable;
  }

  public String getId() {
    return this.id;
  }

  public String getName() {
    return this.name;
  }

  public String getDescription() {
    return this.description;
  }

  public String getInformation() {
    return this.information;
  }

  public String [] getTags() {
    return this.tags;
  }

  public Long getCreationDate() {
    return this.creationDate;
  }

  public Float getGrade() {
    return this.grade;
  }

  public float getDepth() {
    return this.depth;
  }

  public float getHeight() {
    return this.height;
  }

  public float getWidth() {
    return this.width;
  }

  public float getElevation() {
    return this.elevation;
  }

  public float getDropOnTopElevation() {
    return this.dropOnTopElevation;
  }

  public boolean isMovable() {
    return this.movable;
  }

  public boolean isDoorOrWindow() {
    return this.doorOrWindow;
  }

  public Content getIcon() {
    return this.icon;
  }

  public Content getPlanIcon() {
    return this.planIcon;
  }

  public Content getModel() {
    return this.model;
  }

  public float [][] getModelRotation() {

    return deepCopy(this.modelRotation);
  }

  private float [][] deepCopy(float [][] modelRotation) {
    return new float [][] {{modelRotation [0][0], modelRotation [0][1], modelRotation [0][2]},
                           {modelRotation [1][0], modelRotation [1][1], modelRotation [1][2]},
                           {modelRotation [2][0], modelRotation [2][1], modelRotation [2][2]}};
  }

  public String getStaircaseCutOutShape() {
    return this.staircaseCutOutShape;
  }

  public String getCreator() {
    return this.creator;
  }

  public boolean isBackFaceShown() {
    return this.backFaceShown;
  }

  public Integer getColor() {
    return this.color;
  }

  public float getIconYaw() {
    return this.iconYaw;
  }

  public boolean isProportional() {
    return this.proportional;
  }

  public boolean isModifiable() {
    return this.modifiable;
  }

  public boolean isResizable() {
    return this.resizable;    
  }

  public boolean isDeformable() {
    return this.deformable;    
  }

  public boolean isTexturable() {
    return this.texturable;
  }

  public BigDecimal getPrice() {
    return this.price;
  }

  public BigDecimal getValueAddedTaxPercentage() {
    return this.valueAddedTaxPercentage;
  }

  public String getCurrency() {
    return this.currency;
  }

  public FurnitureCategory getCategory() {
    return this.category;
  }

  void setCategory(FurnitureCategory category) {
    this.category = category;
  }

  public boolean equals(Object obj) {
    return super.equals(obj);
  }

  public int hashCode() {
    return super.hashCode();
  }

  public int compareTo(CatalogPieceOfFurniture piece) {
    int nameComparison = COMPARATOR.compare(this.name, piece.name);
    if (nameComparison != 0) {
      return nameComparison;
    } else {
      return this.modifiable == piece.modifiable 
          ? 0
          : (this.modifiable ? 1 : -1); 
    }
  }
  
  public boolean matchesFilter(String filter) {
    byte [][] filterCriteriaCollationKeys = getFilterCollationKeys(filter);
    int checkedCriteria = 0;
    if (filterCriteriaCollationKeys.length > 0) {
      byte [] furnitureCollationKey = getPieceOfFurnitureCollationKey();
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

  private byte [] getPieceOfFurnitureCollationKey() {
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
      if (getDescription() != null) {
        search.append(getDescription());
        search.append('|');
      }
      for (String tag : getTags()) {
        search.append(tag);
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
