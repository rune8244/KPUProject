package com.eteks.homeview3d.model;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Collator;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class HomePieceOfFurniture extends HomeObject implements PieceOfFurniture, Selectable, Elevatable {
  private static final long serialVersionUID = 1L;
  
  private static final double TWICE_PI = 2 * Math.PI;

  public enum Property {NAME, NAME_VISIBLE, NAME_X_OFFSET, NAME_Y_OFFSET, NAME_STYLE, NAME_ANGLE,
      DESCRIPTION, PRICE, WIDTH, DEPTH, HEIGHT, COLOR, TEXTURE, MODEL_MATERIALS, SHININESS, VISIBLE, X, Y, ELEVATION, ANGLE, MODEL_MIRRORED, MOVABLE, LEVEL};

  public enum SortableProperty {CATALOG_ID, NAME, WIDTH, DEPTH, HEIGHT, MOVABLE, 
                                DOOR_OR_WINDOW, COLOR, TEXTURE, VISIBLE, X, Y, ELEVATION, ANGLE,
                                PRICE, VALUE_ADDED_TAX, VALUE_ADDED_TAX_PERCENTAGE, PRICE_VALUE_ADDED_TAX_INCLUDED, LEVEL};
  private static final Map<SortableProperty, Comparator<HomePieceOfFurniture>> SORTABLE_PROPERTY_COMPARATORS;
  private static final float [][] IDENTITY = new float [][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};
  
  static {
    final Collator collator = Collator.getInstance();
    SORTABLE_PROPERTY_COMPARATORS = new HashMap<SortableProperty, Comparator<HomePieceOfFurniture>>();
    SORTABLE_PROPERTY_COMPARATORS.put(SortableProperty.CATALOG_ID, new Comparator<HomePieceOfFurniture>() {
        public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
          if (piece1.catalogId == piece2.catalogId) {
            return 0;
          } else if (piece1.catalogId == null) {
            return -1;
          } else if (piece2.catalogId == null) {
            return 1; 
          } else {
            return collator.compare(piece1.catalogId, piece2.catalogId);
          }
        }
      });
    SORTABLE_PROPERTY_COMPARATORS.put(SortableProperty.NAME, new Comparator<HomePieceOfFurniture>() {
        public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
          if (piece1.name == piece2.name) {
            return 0;
          } else if (piece1.name == null) {
            return -1;
          } else if (piece2.name == null) {
            return 1; 
          } else {
            return collator.compare(piece1.name, piece2.name);
          }
        }
      });
    SORTABLE_PROPERTY_COMPARATORS.put(SortableProperty.WIDTH, new Comparator<HomePieceOfFurniture>() {
        public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
          return HomePieceOfFurniture.compare(piece1.width, piece2.width);
        }
      });
    SORTABLE_PROPERTY_COMPARATORS.put(SortableProperty.HEIGHT, new Comparator<HomePieceOfFurniture>() {
        public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
          return HomePieceOfFurniture.compare(piece1.height, piece2.height);
        }
      });
    SORTABLE_PROPERTY_COMPARATORS.put(SortableProperty.DEPTH, new Comparator<HomePieceOfFurniture>() {
        public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
          return HomePieceOfFurniture.compare(piece1.depth, piece2.depth);
        }
      });
    SORTABLE_PROPERTY_COMPARATORS.put(SortableProperty.MOVABLE, new Comparator<HomePieceOfFurniture>() {
        public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
          return HomePieceOfFurniture.compare(piece1.movable, piece2.movable);
        }
      });
    SORTABLE_PROPERTY_COMPARATORS.put(SortableProperty.DOOR_OR_WINDOW, new Comparator<HomePieceOfFurniture>() {
        public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
          return HomePieceOfFurniture.compare(piece1.doorOrWindow, piece2.doorOrWindow);
        }
      });
    SORTABLE_PROPERTY_COMPARATORS.put(SortableProperty.COLOR, new Comparator<HomePieceOfFurniture>() {
        public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
          if (piece1.color == piece2.color) {
            return 0; 
          } else if (piece1.color == null) {
            return -1;
          } else if (piece2.color == null) {
            return 1; 
          } else {
            return piece1.color - piece2.color;
          }
        }
      });
    SORTABLE_PROPERTY_COMPARATORS.put(SortableProperty.TEXTURE, new Comparator<HomePieceOfFurniture>() {
        public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
          if (piece1.texture == piece2.texture) {
            return 0; 
          } else if (piece1.texture == null) {
            return -1;
          } else if (piece2.texture == null) {
            return 1; 
          } else {
            return collator.compare(piece1.texture.getName(), piece2.texture.getName());
          }
        }
      });
    SORTABLE_PROPERTY_COMPARATORS.put(SortableProperty.VISIBLE, new Comparator<HomePieceOfFurniture>() {
        public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
          return HomePieceOfFurniture.compare(piece1.visible, piece2.visible);
        }
      });
    SORTABLE_PROPERTY_COMPARATORS.put(SortableProperty.X, new Comparator<HomePieceOfFurniture>() {
        public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
          return HomePieceOfFurniture.compare(piece1.x, piece2.x);
        }
      });
    SORTABLE_PROPERTY_COMPARATORS.put(SortableProperty.Y, new Comparator<HomePieceOfFurniture>() {
        public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
          return HomePieceOfFurniture.compare(piece1.y, piece2.y);
        }
      });
    SORTABLE_PROPERTY_COMPARATORS.put(SortableProperty.ELEVATION, new Comparator<HomePieceOfFurniture>() {
        public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
          return HomePieceOfFurniture.compare(piece1.elevation, piece2.elevation);
        }
      });
    SORTABLE_PROPERTY_COMPARATORS.put(SortableProperty.ANGLE, new Comparator<HomePieceOfFurniture>() {
        public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
          return HomePieceOfFurniture.compare(piece1.angle, piece2.angle);
        }
      });
    SORTABLE_PROPERTY_COMPARATORS.put(SortableProperty.LEVEL, new Comparator<HomePieceOfFurniture>() {
        public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
          return HomePieceOfFurniture.compare(piece1.getLevel(), piece2.getLevel());
        }
      });
    SORTABLE_PROPERTY_COMPARATORS.put(SortableProperty.PRICE, new Comparator<HomePieceOfFurniture>() {
        public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
          return HomePieceOfFurniture.compare(piece1.price, piece2.price);
        }
      });
    SORTABLE_PROPERTY_COMPARATORS.put(SortableProperty.VALUE_ADDED_TAX_PERCENTAGE, new Comparator<HomePieceOfFurniture>() {
        public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
          return HomePieceOfFurniture.compare(piece1.valueAddedTaxPercentage, piece2.valueAddedTaxPercentage);
        }
      });
    SORTABLE_PROPERTY_COMPARATORS.put(SortableProperty.VALUE_ADDED_TAX, new Comparator<HomePieceOfFurniture>() {
        public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
          return HomePieceOfFurniture.compare(piece1.getValueAddedTax(), piece2.getValueAddedTax());
        }
      });
    SORTABLE_PROPERTY_COMPARATORS.put(SortableProperty.PRICE_VALUE_ADDED_TAX_INCLUDED, new Comparator<HomePieceOfFurniture>() {
        public int compare(HomePieceOfFurniture piece1, HomePieceOfFurniture piece2) {
          return HomePieceOfFurniture.compare(piece1.getPriceValueAddedTaxIncluded(), piece2.getPriceValueAddedTaxIncluded());
        }
      });
  }
  
  private static int compare(float value1, float value2) {
    return Float.compare(value1, value2);
  }
  
  private static int compare(boolean value1, boolean value2) {
    return value1 == value2 
               ? 0
               : (value1 ? -1 : 1);
  }
  
  private static int compare(BigDecimal value1, BigDecimal value2) {
    if (value1 == value2) {
      return 0;
    } else if (value1 == null) {
      return -1;
    } else if (value2 == null) {
      return 1; 
    } else {
      return value1.compareTo(value2);
    }
  }
  
  private static int compare(Level level1, Level level2) {
    if (level1 == level2) {
      return 0;
    } else if (level1 == null) {
      return -1;
    } else if (level2 == null) {
      return 1; 
    } else {
      return Float.compare(level1.getElevation(), level2.getElevation());
    }
  }
  
  private String                 catalogId;
  private String                 name;
  private boolean                nameVisible;
  private float                  nameXOffset;
  private float                  nameYOffset;
  private TextStyle              nameStyle;
  private float                  nameAngle;
  private String                 description;
  private String                 information;
  private Content                icon;
  private Content                planIcon;
  private Content                model;
  private float                  width;
  private float                  depth;
  private float                  height;
  private float                  elevation;
  private float                  dropOnTopElevation;
  private boolean                movable;
  private boolean                doorOrWindow;
  private HomeMaterial []        modelMaterials;
  private Integer                color;
  private HomeTexture            texture;
  private Float                  shininess;
  private float [][]             modelRotation;
  private String                 staircaseCutOutShape;
  private String                 creator;
  private boolean                backFaceShown;
  private boolean                resizable;
  private boolean                deformable;
  private boolean                texturable;
  private BigDecimal             price;
  private BigDecimal             valueAddedTaxPercentage;
  private String                 currency;
  private boolean                visible;
  private float                  x;
  private float                  y;
  private float                  angle;
  private boolean                modelMirrored;
  private Level                  level;
  
  private transient PropertyChangeSupport propertyChangeSupport = new PropertyChangeSupport(this);
  private transient Shape shapeCache;

  public HomePieceOfFurniture(PieceOfFurniture piece) {
    this.name = piece.getName();
    this.description = piece.getDescription();
    this.information = piece.getInformation();
    this.icon = piece.getIcon();
    this.planIcon = piece.getPlanIcon();
    this.model = piece.getModel();
    this.width = piece.getWidth();
    this.depth = piece.getDepth();
    this.height = piece.getHeight();
    this.elevation = piece.getElevation();
    this.dropOnTopElevation = piece.getDropOnTopElevation();
    this.movable = piece.isMovable();
    this.doorOrWindow = piece.isDoorOrWindow();
    this.color = piece.getColor();
    this.modelRotation = piece.getModelRotation();
    this.staircaseCutOutShape = piece.getStaircaseCutOutShape();
    this.creator = piece.getCreator();
    this.backFaceShown = piece.isBackFaceShown();
    this.resizable = piece.isResizable();
    this.deformable = piece.isDeformable();
    this.texturable = piece.isTexturable();
    this.price = piece.getPrice();
    this.valueAddedTaxPercentage = piece.getValueAddedTaxPercentage();
    this.currency = piece.getCurrency();
    if (piece instanceof HomePieceOfFurniture) {
      HomePieceOfFurniture homePiece = 
          (HomePieceOfFurniture)piece;
      this.catalogId = homePiece.getCatalogId();
      this.nameVisible = homePiece.isNameVisible();
      this.nameXOffset = homePiece.getNameXOffset();
      this.nameYOffset = homePiece.getNameYOffset();
      this.nameAngle = homePiece.getNameAngle();
      this.nameStyle = homePiece.getNameStyle();
      this.visible = homePiece.isVisible();
      this.angle = homePiece.getAngle();
      this.x = homePiece.getX();
      this.y = homePiece.getY();
      this.modelMirrored = homePiece.isModelMirrored();
      this.texture = homePiece.getTexture();
      this.shininess = homePiece.getShininess();
      this.modelMaterials = homePiece.getModelMaterials();
    } else {
      if (piece instanceof CatalogPieceOfFurniture) {
        this.catalogId = ((CatalogPieceOfFurniture)piece).getId();
      }      
      this.visible = true;
      this.x = this.width / 2;
      this.y = this.depth / 2;
    }
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    this.dropOnTopElevation = 1f;
    this.modelRotation = IDENTITY;
    this.resizable = true;
    this.deformable = true;
    this.texturable = true;
    this.propertyChangeSupport = new PropertyChangeSupport(this);
    in.defaultReadObject();
    
    // 각도가 항상 양수이고 0과 2파이 사이인지 확인
    this.angle = (float)((this.angle % TWICE_PI + TWICE_PI) % TWICE_PI);
  }

  public void addPropertyChangeListener(PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(listener);
  }

  public void removePropertyChangeListener(PropertyChangeListener listener) {
    this.propertyChangeSupport.removePropertyChangeListener(listener);
  }

  public String getCatalogId() {
    return this.catalogId;
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    if (name != this.name
        && (name == null || !name.equals(this.name))) {
      String oldName = this.name;
      this.name = name;
      this.propertyChangeSupport.firePropertyChange(Property.NAME.name(), oldName, name);
    }
  }

  public boolean isNameVisible() {
    return this.nameVisible;  
  }
  
  public void setNameVisible(boolean nameVisible) {
    if (nameVisible != this.nameVisible) {
      this.nameVisible = nameVisible;
      this.propertyChangeSupport.firePropertyChange(Property.NAME_VISIBLE.name(), !nameVisible, nameVisible);
    }
  }

  public float getNameXOffset() {
    return this.nameXOffset;  
  }

  public void setNameXOffset(float nameXOffset) {
    if (nameXOffset != this.nameXOffset) {
      float oldNameXOffset = this.nameXOffset;
      this.nameXOffset = nameXOffset;
      this.propertyChangeSupport.firePropertyChange(Property.NAME_X_OFFSET.name(), oldNameXOffset, nameXOffset);
    }
  }

  public float getNameYOffset() {
    return this.nameYOffset;  
  }

  public void setNameYOffset(float nameYOffset) {
    if (nameYOffset != this.nameYOffset) {
      float oldNameYOffset = this.nameYOffset;
      this.nameYOffset = nameYOffset;
      this.propertyChangeSupport.firePropertyChange(Property.NAME_Y_OFFSET.name(), oldNameYOffset, nameYOffset);
    }
  }

  public TextStyle getNameStyle() {
    return this.nameStyle;  
  }

  public void setNameStyle(TextStyle nameStyle) {
    if (nameStyle != this.nameStyle) {
      TextStyle oldNameStyle = this.nameStyle;
      this.nameStyle = nameStyle;
      this.propertyChangeSupport.firePropertyChange(Property.NAME_STYLE.name(), oldNameStyle, nameStyle);
    }
  }

  public float getNameAngle() {
    return this.nameAngle;
  }

  public void setNameAngle(float nameAngle) {
    nameAngle = (float)((nameAngle % TWICE_PI + TWICE_PI) % TWICE_PI);
    if (nameAngle != this.nameAngle) {
      float oldNameAngle = this.nameAngle;
      this.nameAngle = nameAngle;
      this.propertyChangeSupport.firePropertyChange(Property.NAME_ANGLE.name(), oldNameAngle, nameAngle);
    }
  }

  public String getDescription() {
    return this.description;
  }

  public void setDescription(String description) {
    if (description != this.description
        && (description == null || !description.equals(this.description))) {
      String oldDescription = this.description;
      this.description = description;
      this.propertyChangeSupport.firePropertyChange(Property.DESCRIPTION.name(), oldDescription, description);
    }
  }

  public String getInformation() {
    return this.information;
  }

  public float getDepth() {
    return this.depth;
  }

  public void setDepth(float depth) {
    if (isResizable()) {
      if (depth != this.depth) {
        float oldDepth = this.depth;
        this.depth = depth;
        this.shapeCache = null;
        this.propertyChangeSupport.firePropertyChange(Property.DEPTH.name(), oldDepth, depth);
      }
    } else {
      throw new IllegalStateException("Piece isn't resizable");
    }
  }

  public float getHeight() {
    return this.height;
  }

  public void setHeight(float height) {
    if (isResizable()) {
      if (height != this.height) {
        float oldHeight = this.height;
        this.height = height;
        this.propertyChangeSupport.firePropertyChange(Property.HEIGHT.name(), oldHeight, height);
      }
    } else {
      throw new IllegalStateException("Piece isn't resizable");
    }
  }

  public float getWidth() {
    return this.width;
  }

  public void setWidth(float width) {
    if (isResizable()) {
      if (width != this.width) {
        float oldWidth = this.width;
        this.width = width;
        this.shapeCache = null;
        this.propertyChangeSupport.firePropertyChange(Property.WIDTH.name(), oldWidth, width);
      }
    } else {
      throw new IllegalStateException("Piece isn't resizable");
    }
  }

  public float getElevation() {
    return this.elevation;
  }

  public float getDropOnTopElevation() {
    return this.dropOnTopElevation;
  }

  public float getGroundElevation() {
    if (this.level != null) {
      return this.elevation + this.level.getElevation();
    } else {
      return this.elevation;
    }
  }

  public void setElevation(float elevation) {
    if (elevation != this.elevation) {
      float oldElevation = this.elevation;
      this.elevation = elevation;
      this.propertyChangeSupport.firePropertyChange(Property.ELEVATION.name(), oldElevation, elevation);
    }
  }

  public boolean isMovable() {
    return this.movable;
  }

  public void setMovable(boolean movable) {
    if (movable != this.movable) {
      this.movable = movable;
      this.propertyChangeSupport.firePropertyChange(Property.MOVABLE.name(), !movable, movable);
    }
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

  public void setModelMaterials(HomeMaterial [] modelMaterials) {
    if (isTexturable()) {
      if (!Arrays.equals(modelMaterials, this.modelMaterials)) {
        HomeMaterial [] oldModelMaterials = this.modelMaterials;
        this.modelMaterials = modelMaterials != null 
            ? modelMaterials.clone()
            : null;
        this.propertyChangeSupport.firePropertyChange(Property.MODEL_MATERIALS.name(), oldModelMaterials, modelMaterials);
      }
    } else {
      throw new IllegalStateException("Piece isn't texturable");
    }
  }

  public HomeMaterial [] getModelMaterials() {
    if (this.modelMaterials != null) {
      return this.modelMaterials.clone();
    } else {
      return null;
    }
  }

  public Integer getColor() {
    return this.color;
  }

  public void setColor(Integer color) {
    if (isTexturable()) {
      if (color != this.color
          && (color == null || !color.equals(this.color))) {
        Integer oldColor = this.color;
        this.color = color;
        this.propertyChangeSupport.firePropertyChange(Property.COLOR.name(), oldColor, color);
      }
    } else {
      throw new IllegalStateException("Piece isn't texturable");
    }
  }

  public HomeTexture getTexture() {
    return this.texture;
  }
  
  public void setTexture(HomeTexture texture) {
    if (isTexturable()) {
      if (texture != this.texture
          && (texture == null || !texture.equals(this.texture))) {
        HomeTexture oldTexture = this.texture;
        this.texture = texture;
        this.propertyChangeSupport.firePropertyChange(Property.TEXTURE.name(), oldTexture, texture);
      }
    } else {
      throw new IllegalStateException("Piece isn't texturable");
    }
  }

  public Float getShininess() {
    return this.shininess;
  }

  public void setShininess(Float shininess) {
    if (isTexturable()) {
      if (shininess != this.shininess
          && (shininess == null || !shininess.equals(this.shininess))) {
        Float oldShininess = this.shininess;
        this.shininess = shininess;
        this.propertyChangeSupport.firePropertyChange(Property.SHININESS.name(), oldShininess, shininess);
      }
    } else {
      throw new IllegalStateException("Piece isn't texturable");
    }
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

  public void setPrice(BigDecimal price) {
    if (price != this.price
        && (price == null || !price.equals(this.price))) {
      BigDecimal oldPrice = this.price;
      this.price = price;
      this.propertyChangeSupport.firePropertyChange(Property.PRICE.name(), oldPrice, price);
    }
  }

  public BigDecimal getValueAddedTaxPercentage() {
    return this.valueAddedTaxPercentage;
  }

  public BigDecimal getValueAddedTax() {
    if (this.price != null && this.valueAddedTaxPercentage != null) {
      return this.price.multiply(this.valueAddedTaxPercentage).
          setScale(this.price.scale(), RoundingMode.HALF_UP);
    } else {
      return null;
    }
  }

  public BigDecimal getPriceValueAddedTaxIncluded() {
    if (this.price != null && this.valueAddedTaxPercentage != null) {
      return this.price.add(getValueAddedTax());
    } else {
      return this.price;
    }
  }

  public String getCurrency() {
    return this.currency;
  }

  public boolean isVisible() {
    return this.visible;
  }
  
  public void setVisible(boolean visible) {
    if (visible != this.visible) {
      this.visible = visible;
      this.propertyChangeSupport.firePropertyChange(Property.VISIBLE.name(), !visible, visible);
    }
  }

  public float getX() {
    return this.x;
  }

  public void setX(float x) {
    if (x != this.x) {
      float oldX = this.x;
      this.x = x;
      this.shapeCache = null;
      this.propertyChangeSupport.firePropertyChange(Property.X.name(), oldX, x);
    }
  }

  public float getY() {
    return this.y;
  }

  public void setY(float y) {
    if (y != this.y) {
      float oldY = this.y;
      this.y = y;
      this.shapeCache = null;
      this.propertyChangeSupport.firePropertyChange(Property.Y.name(), oldY, y);
    }
  }

  public float getAngle() {
    return this.angle;
  }

  public void setAngle(float angle) {
    angle = (float)((angle % TWICE_PI + TWICE_PI) % TWICE_PI);
    if (angle != this.angle) {
      float oldAngle = this.angle;
      this.angle = angle;
      this.shapeCache = null;
      this.propertyChangeSupport.firePropertyChange(Property.ANGLE.name(), oldAngle, angle);
    }
  }

  public boolean isModelMirrored() {
    return this.modelMirrored;
  }

  public void setModelMirrored(boolean modelMirrored) {
    if (isResizable()) {
      if (modelMirrored != this.modelMirrored) {
        this.modelMirrored = modelMirrored;
        this.propertyChangeSupport.firePropertyChange(Property.MODEL_MIRRORED.name(), 
            !modelMirrored, modelMirrored);
      }
    } else {
      throw new IllegalStateException("Piece isn't resizable");
    }
  }

  public float [][] getModelRotation() {
    return new float [][] {{this.modelRotation[0][0], this.modelRotation[0][1], this.modelRotation[0][2]},
                           {this.modelRotation[1][0], this.modelRotation[1][1], this.modelRotation[1][2]},
                           {this.modelRotation[2][0], this.modelRotation[2][1], this.modelRotation[2][2]}};
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

  public Level getLevel() {
    return this.level;
  }

  public void setLevel(Level level) {
    if (level != this.level) {
      Level oldLevel = this.level;
      this.level = level;
      this.propertyChangeSupport.firePropertyChange(Property.LEVEL.name(), oldLevel, level);
    }
  }

  public boolean isAtLevel(Level level) {
    if (this.level == level) {
      return true;
    } else if (this.level != null && level != null) {
      float pieceLevelElevation = this.level.getElevation();
      float levelElevation = level.getElevation();
      return pieceLevelElevation == levelElevation
             && this.level.getElevationIndex() < level.getElevationIndex()
          || pieceLevelElevation < levelElevation
             && isTopAtLevel(level);
    } else {
      return false;
    }
  }
  
  private boolean isTopAtLevel(Level level) {
    float topElevation = this.level.getElevation() + this.elevation + this.height;
    if (this.staircaseCutOutShape != null) {
      return topElevation >= level.getElevation();
    } else {
      return topElevation > level.getElevation();
    }
  }

  public float [][] getPoints() {
    float [][] piecePoints = new float[4][2];
    PathIterator it = getShape().getPathIterator(null);
    for (int i = 0; i < piecePoints.length; i++) {
      it.currentSegment(piecePoints [i]);
      it.next();
    }
    return piecePoints;
  }

  public boolean intersectsRectangle(float x0, float y0, 
                                     float x1, float y1) {
    Rectangle2D rectangle = new Rectangle2D.Float(x0, y0, 0, 0);
    rectangle.add(x1, y1);
    return getShape().intersects(rectangle);
  }

  public boolean containsPoint(float x, float y, float margin) {
    if (margin == 0) {
      return getShape().contains(x, y);
    } else {
      return getShape().intersects(x - margin, y - margin, 2 * margin, 2 * margin);
    }
  }

  public boolean isPointAt(float x, float y, float margin) {
    for (float [] point : getPoints()) {
      if (Math.abs(x - point[0]) <= margin && Math.abs(y - point[1]) <= margin) {
        return true;
      }
    } 
    return false;
  }

  public boolean isTopLeftPointAt(float x, float y, float margin) {
    float [][] points = getPoints();
    double distanceSquareToTopLeftPoint = Point2D.distanceSq(x, y, points[0][0], points[0][1]);
    return distanceSquareToTopLeftPoint <= margin * margin
        && distanceSquareToTopLeftPoint < Point2D.distanceSq(x, y, points[1][0], points[1][1])
        && distanceSquareToTopLeftPoint < Point2D.distanceSq(x, y, points[3][0], points[3][1]);
  }

  public boolean isTopRightPointAt(float x, float y, float margin) {
    float [][] points = getPoints();
    double distanceSquareToTopRightPoint = Point2D.distanceSq(x, y, points[1][0], points[1][1]);
    return distanceSquareToTopRightPoint <= margin * margin
        && distanceSquareToTopRightPoint < Point2D.distanceSq(x, y, points[0][0], points[0][1])
        && distanceSquareToTopRightPoint < Point2D.distanceSq(x, y, points[2][0], points[2][1]);
  }

  public boolean isBottomLeftPointAt(float x, float y, float margin) {
    float [][] points = getPoints();
    double distanceSquareToBottomLeftPoint = Point2D.distanceSq(x, y, points[3][0], points[3][1]);
    return distanceSquareToBottomLeftPoint <= margin * margin
        && distanceSquareToBottomLeftPoint < Point2D.distanceSq(x, y, points[0][0], points[0][1])
        && distanceSquareToBottomLeftPoint < Point2D.distanceSq(x, y, points[2][0], points[2][1]);
  }

  public boolean isBottomRightPointAt(float x, float y, float margin) {
    float [][] points = getPoints();
    double distanceSquareToBottomRightPoint = Point2D.distanceSq(x, y, points[2][0], points[2][1]);
    return distanceSquareToBottomRightPoint <= margin * margin
        && distanceSquareToBottomRightPoint < Point2D.distanceSq(x, y, points[1][0], points[1][1])
        && distanceSquareToBottomRightPoint < Point2D.distanceSq(x, y, points[3][0], points[3][1]);
  }

  public boolean isNameCenterPointAt(float x, float y, float margin) {
    return Math.abs(x - getX() - getNameXOffset()) <= margin 
        && Math.abs(y - getY() - getNameYOffset()) <= margin;
  }

  private Shape getShape() {
    if (this.shapeCache == null) {
      Rectangle2D pieceRectangle = new Rectangle2D.Float(
          getX() - getWidth() / 2,
          getY() - getDepth() / 2,
          getWidth(), getDepth());
      AffineTransform rotation = AffineTransform.getRotateInstance(getAngle(), getX(), getY());
      PathIterator it = pieceRectangle.getPathIterator(rotation);
      GeneralPath pieceShape = new GeneralPath();
      pieceShape.append(it, false);
      this.shapeCache = pieceShape;
    }
    return this.shapeCache;
  }

  public void move(float dx, float dy) {
    setX(getX() + dx);
    setY(getY() + dy);
  }

  public HomePieceOfFurniture clone() {
    HomePieceOfFurniture clone = (HomePieceOfFurniture)super.clone();
    clone.propertyChangeSupport = new PropertyChangeSupport(clone);
    clone.level = null;
    return clone;
  }

  public static Comparator<HomePieceOfFurniture> getFurnitureComparator(SortableProperty property) {
    return SORTABLE_PROPERTY_COMPARATORS.get(property);    
  }
}
