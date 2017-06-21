package com.eteks.homeview3d.model;

import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFurnitureGroup extends HomePieceOfFurniture {
  private static final long serialVersionUID = 1L;
  
  private static final float [][] IDENTITY = new float [][] {{1, 0, 0}, {0, 1, 0}, {0, 0, 1}};

  private List<HomePieceOfFurniture> furniture;
  private boolean                    resizable;
  private boolean                    deformable;
  private boolean                    texturable;
  private boolean                    doorOrWindow;
  private float                      fixedWidth;
  private float                      fixedDepth;
  private float                      fixedHeight;
  private float                      dropOnTopElevation; 
  private String                     currency;

  private transient PropertyChangeListener furnitureListener;

  public HomeFurnitureGroup(List<HomePieceOfFurniture> furniture,
                            String name) {
    this(furniture, furniture.get(0), name);
  }

  public HomeFurnitureGroup(List<HomePieceOfFurniture> furniture,
                            HomePieceOfFurniture leadingPiece,
                            String name) {
    this(furniture, leadingPiece.getAngle(), false, name);
  }

  public HomeFurnitureGroup(List<HomePieceOfFurniture> furniture,
                            float angle, boolean modelMirrored,
                            String name) {
    super(furniture.get(0));
    this.furniture = Collections.unmodifiableList(furniture); 
    
    boolean movable = true;
    boolean visible = false;
    for (HomePieceOfFurniture piece : furniture) {
      movable &= piece.isMovable();
      visible |= piece.isVisible();
    }
    
    setName(name);
    setNameVisible(false);
    setNameXOffset(0);
    setNameYOffset(0);
    setNameStyle(null);
    setDescription(null);
    super.setMovable(movable);
    setVisible(visible);
   
    updateLocationAndSize(furniture, angle, true);
    super.setAngle(angle);
    super.setModelMirrored(modelMirrored);

    addFurnitureListener();
  }

  private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
    this.dropOnTopElevation = -1;
    this.deformable = true;
    this.texturable = true;
    in.defaultReadObject();
    addFurnitureListener();
  }

  private void updateLocationAndSize(List<HomePieceOfFurniture> furniture,
                                     float angle,
                                     boolean init) {
    this.resizable = true;
    this.deformable = true;
    this.texturable = true;
    this.doorOrWindow = true;
    this.currency = furniture.get(0).getCurrency();
    for (HomePieceOfFurniture piece : furniture) {
      this.resizable &= piece.isResizable();
      this.deformable &= piece.isDeformable();
      this.texturable &= piece.isTexturable();
      this.doorOrWindow &= piece.isDoorOrWindow();
      if (this.currency != null) {
        if (piece.getCurrency() == null
            || !piece.getCurrency().equals(this.currency)) {
          this.currency = null; 
        }
      }
    }

    float elevation = Float.MAX_VALUE;
    if (init) {
      Level minLevel = null;
      for (HomePieceOfFurniture piece : furniture) {
        Level level = piece.getLevel();
        if (level != null 
            && (minLevel == null
                || level.getElevation() < minLevel.getElevation())) {
          minLevel = level;
        }
      }
      for (HomePieceOfFurniture piece : furniture) {
        if (piece.getLevel() != null) {
          elevation = Math.min(elevation, piece.getGroundElevation() - minLevel.getElevation());
          piece.setElevation(piece.getGroundElevation() - minLevel.getElevation());
          piece.setLevel(null);
        } else {
          elevation = Math.min(elevation, piece.getElevation());
        }
      }
    } else {
      for (HomePieceOfFurniture piece : furniture) {
        elevation = Math.min(elevation, piece.getElevation());
      }
    }

    float height = 0;
    float dropOnTopElevation = -1;
    for (HomePieceOfFurniture piece : furniture) {
      height = Math.max(height, piece.getElevation() + piece.getHeight());
      if (piece.getDropOnTopElevation() >= 0) {
        dropOnTopElevation = Math.max(dropOnTopElevation, 
            piece.getElevation() + piece.getHeight() * piece.getDropOnTopElevation());
      }
    }
    height -= elevation;
    dropOnTopElevation -= elevation;

    AffineTransform rotation = AffineTransform.getRotateInstance(-angle);
    Rectangle2D unrotatedBoundingRectangle = null;
    for (HomePieceOfFurniture piece : getFurnitureWithoutGroups(furniture)) {
      GeneralPath pieceShape = new GeneralPath();
      float [][] points = piece.getPoints();
      pieceShape.moveTo(points [0][0], points [0][1]);
      for (int i = 1; i < points.length; i++) {
        pieceShape.lineTo(points [i][0], points [i][1]);
      }
      pieceShape.closePath();
      if (unrotatedBoundingRectangle == null) {
        unrotatedBoundingRectangle = pieceShape.createTransformedShape(rotation).getBounds2D();
      } else {
        unrotatedBoundingRectangle.add(pieceShape.createTransformedShape(rotation).getBounds2D());
      }
    }
    Point2D center = new Point2D.Float((float)unrotatedBoundingRectangle.getCenterX(), (float)unrotatedBoundingRectangle.getCenterY());
    rotation.setToRotation(angle);
    rotation.transform(center, center);

    if (this.resizable) {
      super.setWidth((float)unrotatedBoundingRectangle.getWidth());
      super.setDepth((float)unrotatedBoundingRectangle.getHeight());
      super.setHeight(height);
    } else {
      this.fixedWidth = (float)unrotatedBoundingRectangle.getWidth();
      this.fixedDepth = (float)unrotatedBoundingRectangle.getHeight();
      this.fixedHeight = height;
    }
    this.dropOnTopElevation = dropOnTopElevation / height;
    super.setX((float)center.getX());
    super.setY((float)center.getY());
    super.setElevation(elevation);
  }
  
  private void addFurnitureListener() {
    this.furnitureListener = new LocationAndSizeChangeListener(this);
    for (HomePieceOfFurniture piece : this.furniture) {
      piece.addPropertyChangeListener(this.furnitureListener);
    }    
  }

  private static class LocationAndSizeChangeListener implements PropertyChangeListener { 
    private WeakReference<HomeFurnitureGroup> group;
    
    public LocationAndSizeChangeListener(HomeFurnitureGroup group) {
      this.group = new WeakReference<HomeFurnitureGroup>(group);
    }
    
    public void propertyChange(PropertyChangeEvent ev) {
      final HomeFurnitureGroup group = this.group.get();
      if (group == null) {
        ((HomePieceOfFurniture)ev.getSource()).removePropertyChangeListener(this);
      } else if (HomePieceOfFurniture.Property.X.name().equals(ev.getPropertyName())
          || HomePieceOfFurniture.Property.Y.name().equals(ev.getPropertyName())
          || HomePieceOfFurniture.Property.ELEVATION.name().equals(ev.getPropertyName())
          || HomePieceOfFurniture.Property.ANGLE.name().equals(ev.getPropertyName())
          || HomePieceOfFurniture.Property.WIDTH.name().equals(ev.getPropertyName())
          || HomePieceOfFurniture.Property.DEPTH.name().equals(ev.getPropertyName())
          || HomePieceOfFurniture.Property.HEIGHT.name().equals(ev.getPropertyName())) {
        group.updateLocationAndSize(group.getFurniture(), group.getAngle(), false);
      }
    }
  }

  private List<HomePieceOfFurniture> getFurnitureWithoutGroups(List<HomePieceOfFurniture> furniture) {
    List<HomePieceOfFurniture> pieces = new ArrayList<HomePieceOfFurniture>();
    for (HomePieceOfFurniture piece : furniture) {
      if (piece instanceof HomeFurnitureGroup) {
        pieces.addAll(getFurnitureWithoutGroups(((HomeFurnitureGroup)piece).getFurniture()));
      } else {
        pieces.add(piece);
      }
    }
    return pieces;
  }

  public List<HomePieceOfFurniture> getAllFurniture() {
    List<HomePieceOfFurniture> pieces = new ArrayList<HomePieceOfFurniture>(this.furniture);
    for (HomePieceOfFurniture piece : getFurniture()) {
      if (piece instanceof HomeFurnitureGroup) {
        pieces.addAll(((HomeFurnitureGroup)piece).getAllFurniture());
      } 
    }
    return pieces;
  }

  public List<HomePieceOfFurniture> getFurniture() {
    return Collections.unmodifiableList(this.furniture);
  }

  void addPieceOfFurniture(HomePieceOfFurniture piece, int index) {
    this.furniture = new ArrayList<HomePieceOfFurniture>(this.furniture);
    piece.setLevel(getLevel());
    this.furniture.add(index, piece);
    piece.addPropertyChangeListener(this.furnitureListener);
    updateLocationAndSize(this.furniture, getAngle(), false);
  }

  void deletePieceOfFurniture(HomePieceOfFurniture piece) {
    int index = this.furniture.indexOf(piece);
    if (index != -1) {
      if (this.furniture.size() > 1) {
        piece.setLevel(null);
        piece.removePropertyChangeListener(this.furnitureListener);
        this.furniture = new ArrayList<HomePieceOfFurniture>(this.furniture);
        this.furniture.remove(index);
        updateLocationAndSize(this.furniture, getAngle(), false);
      } else {
        throw new IllegalStateException("Group can't be empty");
      }
    }
  }

  public String getCatalogId() {
    return null;
  }

  public String getInformation() {
    return null;
  }

  public boolean isMovable() {
    return super.isMovable();
  }

  public void setMovable(boolean movable) {
    super.setMovable(movable);
  }

  public boolean isDoorOrWindow() {
    return this.doorOrWindow;
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

  public float getWidth() {
    if (!this.resizable) {
      return this.fixedWidth;
    } else {
      return super.getWidth();
    }
  }

  public float getDepth() {
    if (!this.resizable) {
      return this.fixedDepth;
    } else {
      return super.getDepth();
    }
  }

  public float getHeight() {
    if (!this.resizable) {
      return this.fixedHeight;
    } else {
      return super.getHeight();
    }
  }

  public float getDropOnTopElevation() {
    return this.dropOnTopElevation;
  }

  public Content getIcon() {
    return null;
  }

  public Content getPlanIcon() {
    return null;
  }

  public Content getModel() {
    return null;
  }

  public float [][] getModelRotation() {
    return IDENTITY;
  }

  public String getStaircaseCutOutShape() {
    return null;
  }

  public String getCreator() {
    return null;
  }

  public BigDecimal getPrice() {
    BigDecimal price = null;
    for (HomePieceOfFurniture piece : this.furniture) {
      if (piece.getPrice() != null) {
        if (price == null) {
          price = piece.getPrice();
        } else {
          price = price.add(piece.getPrice()); 
        }
      }
    }
    if (price == null) {
      return super.getPrice();
    } else {
      return price;
    }
  }

  public void setPrice(BigDecimal price) {
    for (HomePieceOfFurniture piece : this.furniture) {
      if (piece.getPrice() != null) {
        throw new UnsupportedOperationException("Can't change the price of a group containing pieces with a price");
      }
    }
    super.setPrice(price);
  }

  public BigDecimal getValueAddedTaxPercentage() {
    BigDecimal valueAddedTaxPercentage = this.furniture.get(0).getValueAddedTaxPercentage();
    if (valueAddedTaxPercentage != null) {
      for (HomePieceOfFurniture piece : this.furniture) {
        BigDecimal pieceValueAddedTaxPercentage = piece.getValueAddedTaxPercentage();
        if (pieceValueAddedTaxPercentage == null
            || !pieceValueAddedTaxPercentage.equals(valueAddedTaxPercentage)) {
          return null; 
        }
      }
    }
    return valueAddedTaxPercentage;
  }

  public String getCurrency() {
    return this.currency;
  }

  public BigDecimal getValueAddedTax() {
    BigDecimal valueAddedTax = null;
    for (HomePieceOfFurniture piece : furniture) {
      BigDecimal pieceValueAddedTax = piece.getValueAddedTax();
      if (pieceValueAddedTax != null) {
        if (valueAddedTax == null) {
          valueAddedTax = pieceValueAddedTax;
        } else {
          valueAddedTax = valueAddedTax.add(pieceValueAddedTax);
        }
      }
    }
    return valueAddedTax;
  }

  public BigDecimal getPriceValueAddedTaxIncluded() {
    BigDecimal priceValueAddedTaxIncluded = null;
    for (HomePieceOfFurniture piece : furniture) {
      if (piece.getPrice() != null) {
        if (priceValueAddedTaxIncluded == null) {
          priceValueAddedTaxIncluded = piece.getPriceValueAddedTaxIncluded();
        } else {
          priceValueAddedTaxIncluded = priceValueAddedTaxIncluded.add(piece.getPriceValueAddedTaxIncluded());
        }
      }
    }
    return priceValueAddedTaxIncluded;
  }

  public boolean isBackFaceShown() {
    return false;
  }

  public Integer getColor() {
    return null;
  }

  public void setColor(Integer color) {
    if (isTexturable()) {
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.setColor(color);
      }
    } 
  }

  public HomeTexture getTexture() {
    return null;
  }

  public void setTexture(HomeTexture texture) {
    if (isTexturable()) {
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.setTexture(texture);
      }
    } 
  }

  public HomeMaterial [] getModelMaterials() {
    return null;
  }

  public void setModelMaterials(HomeMaterial [] modelMaterials) {
    if (isTexturable()) {
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.setModelMaterials(modelMaterials);
      }
    } 
  }

  public Float getShininess() {
    return null;
  }
  
  public void setShininess(Float shininess) {
    if (isTexturable()) {
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.setShininess(shininess);
      }
    } 
  }

  public void setAngle(float angle) {
    if (angle != getAngle()) {
      float angleDelta = angle - getAngle();
      double cosAngleDelta = Math.cos(angleDelta);
      double sinAngleDelta = Math.sin(angleDelta);
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.removePropertyChangeListener(this.furnitureListener);
        piece.setAngle(piece.getAngle() + angleDelta);     
        float newX = getX() + (float)((piece.getX() - getX()) * cosAngleDelta - (piece.getY() - getY()) * sinAngleDelta);
        float newY = getY() + (float)((piece.getX() - getX()) * sinAngleDelta + (piece.getY() - getY()) * cosAngleDelta);
        piece.setX(newX);
        piece.setY(newY);
        piece.addPropertyChangeListener(this.furnitureListener);
      }
      super.setAngle(angle);
    }
  }

  public void setX(float x) {
    if (x != getX()) {
      float dx = x - getX();
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.removePropertyChangeListener(this.furnitureListener);
        piece.setX(piece.getX() + dx);
        piece.addPropertyChangeListener(this.furnitureListener);
      }
      super.setX(x);
    }
  }

  public void setY(float y) {
    if (y != getY()) {
      float dy = y - getY();
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.addPropertyChangeListener(this.furnitureListener);
        piece.setY(piece.getY() + dy);
        piece.removePropertyChangeListener(this.furnitureListener);
      }
      super.setY(y);
    }
  }

  public void setWidth(float width) {
    if (width != getWidth()) {
      float widthFactor = width / getWidth();
      float angle = getAngle();
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.removePropertyChangeListener(this.furnitureListener);
        float angleDelta = piece.getAngle() - angle;
        float pieceWidth = piece.getWidth();
        float pieceDepth = piece.getDepth();
        piece.setWidth(pieceWidth + pieceWidth * (widthFactor - 1) * Math.abs((float)Math.cos(angleDelta)));
        piece.setDepth(pieceDepth + pieceDepth * (widthFactor - 1) * Math.abs((float)Math.sin(angleDelta)));       
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);
        float newX = getX() + (float)((piece.getX() - getX()) * cosAngle + (piece.getY() - getY()) * sinAngle);
        float newY = getY() + (float)((piece.getX() - getX()) * -sinAngle + (piece.getY() - getY()) * cosAngle);
        newX = getX() + (newX - getX()) * widthFactor; 
        piece.setX(getX() + (float)((newX - getX()) * cosAngle - (newY - getY()) * sinAngle));
        piece.setY(getY() + (float)((newX - getX()) * sinAngle + (newY - getY()) * cosAngle));
        piece.addPropertyChangeListener(this.furnitureListener);
      }
      super.setWidth(width);
    }
  }

  public void setDepth(float depth) {
    if (depth != getDepth()) {
      float depthFactor = depth / getDepth();
      float angle = getAngle();
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.removePropertyChangeListener(this.furnitureListener);
        float angleDelta = piece.getAngle() - angle;
        float pieceWidth = piece.getWidth();
        float pieceDepth = piece.getDepth();
        piece.setWidth(pieceWidth + pieceWidth * (depthFactor - 1) * Math.abs((float)Math.sin(angleDelta)));
        piece.setDepth(pieceDepth + pieceDepth * (depthFactor - 1) * Math.abs((float)Math.cos(angleDelta)));
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);
        float newX = getX() + (float)((piece.getX() - getX()) * cosAngle + (piece.getY() - getY()) * sinAngle);
        float newY = getY() + (float)((piece.getX() - getX()) * -sinAngle + (piece.getY() - getY()) * cosAngle);
        newY = getY() + (newY - getY()) * depthFactor;
        piece.setX(getX() + (float)((newX - getX()) * cosAngle - (newY - getY()) * sinAngle));
        piece.setY(getY() + (float)((newX - getX()) * sinAngle + (newY - getY()) * cosAngle));
        piece.addPropertyChangeListener(this.furnitureListener);
      }
      super.setDepth(depth);
    }
  }

  public void setHeight(float height) {
    if (height != getHeight()) {
      float heightFactor = height / getHeight();
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.removePropertyChangeListener(this.furnitureListener);
        piece.setHeight(piece.getHeight() * heightFactor);
        piece.setElevation(getElevation() 
            + (piece.getElevation() - getElevation()) * heightFactor);
        piece.addPropertyChangeListener(this.furnitureListener);
      }
      super.setHeight(height);
    }
  }

  public void setElevation(float elevation) {
    if (elevation != getElevation()) {
      float elevationDelta = elevation - getElevation();
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.removePropertyChangeListener(this.furnitureListener);
        piece.setElevation(piece.getElevation() + elevationDelta);
        piece.addPropertyChangeListener(this.furnitureListener);
      }
      super.setElevation(elevation);
    }
  }

  public void setModelMirrored(boolean modelMirrored) {
    if (modelMirrored != isModelMirrored()) {
      float angle = getAngle();
      for (HomePieceOfFurniture piece : this.furniture) {
        piece.removePropertyChangeListener(this.furnitureListener);
        piece.setModelMirrored(!piece.isModelMirrored());
        double cosAngle = Math.cos(angle);
        double sinAngle = Math.sin(angle);
        float newX = getX() + (float)((piece.getX() - getX()) * cosAngle + (piece.getY() - getY()) * sinAngle);
        float newY = getY() + (float)((piece.getX() - getX()) * -sinAngle + (piece.getY() - getY()) * cosAngle);
        newX = getX() - (newX - getX()); 
        piece.setX(getX() + (float)((newX - getX()) * cosAngle - (newY - getY()) * sinAngle));
        piece.setY(getY() + (float)((newX - getX()) * sinAngle + (newY - getY()) * cosAngle));
        piece.addPropertyChangeListener(this.furnitureListener);
      }
      super.setModelMirrored(modelMirrored);
    }
  }
 
  public void setVisible(boolean visible) {
    for (HomePieceOfFurniture piece : this.furniture) {
      piece.setVisible(visible);
    }
    super.setVisible(visible);
  }

  public void setLevel(Level level) {
    for (HomePieceOfFurniture piece : this.furniture) {
      piece.setLevel(level);
    }
    super.setLevel(level);
  }

  public boolean intersectsRectangle(float x0, float y0, 
                                     float x1, float y1) {
    for (HomePieceOfFurniture piece : this.furniture) {
      if (piece.intersectsRectangle(x0, y0, x1, y1)) {
        return true;
      }
    }
    return false;
  }

  public boolean containsPoint(float x, float y, float margin) {
    for (HomePieceOfFurniture piece : this.furniture) {
      if (piece.containsPoint(x, y, margin)) {
        return true;
      }
    }
    return false;
  }

  public HomeFurnitureGroup clone() {
    HomeFurnitureGroup clone = (HomeFurnitureGroup)super.clone();
    clone.furniture = new ArrayList<HomePieceOfFurniture>(this.furniture.size());
    for (HomePieceOfFurniture piece : this.furniture) {
      HomePieceOfFurniture pieceClone = piece.clone();
      clone.furniture.add(pieceClone);
      if (piece instanceof HomeDoorOrWindow
          && ((HomeDoorOrWindow)piece).isBoundToWall()) {
        ((HomeDoorOrWindow)pieceClone).setBoundToWall(true);
      }
    }
    clone.furniture = Collections.unmodifiableList(clone.furniture);
    clone.addFurnitureListener();
    return clone;
  }
}

