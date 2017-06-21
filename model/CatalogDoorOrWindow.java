package com.eteks.homeview3d.model;

import java.math.BigDecimal;

public class CatalogDoorOrWindow extends CatalogPieceOfFurniture implements DoorOrWindow {
  private final float   wallThickness;
  private final float   wallDistance;
  private final Sash [] sashes;
  private final String  cutOutShape;

  public CatalogDoorOrWindow(String id, String name, String description, Content icon, Content model, 
                             float width, float depth, float height, float elevation, boolean movable, 
                             float wallThickness, float wallDistance, Sash [] sashes,
                             float [][] modelRotation, String creator,
                             boolean resizable, BigDecimal price, BigDecimal valueAddedTaxPercentage) {
    this(id, name, description, icon, null, model, width, depth, height, elevation, movable,   
        wallThickness, wallDistance, sashes, modelRotation, creator, resizable, price, valueAddedTaxPercentage);
  }
         
  public CatalogDoorOrWindow(String id, String name, String description, 
                             Content icon, Content planIcon, Content model, 
                             float width, float depth, float height, float elevation, boolean movable, 
                             float wallThickness, float wallDistance, Sash [] sashes,
                             float [][] modelRotation, String creator,
                             boolean resizable, BigDecimal price, BigDecimal valueAddedTaxPercentage) {
    this(id, name, description, icon, planIcon, model, width, depth, height, elevation, movable,   
        wallThickness, wallDistance, sashes,
        modelRotation, creator, resizable, true, true, price, valueAddedTaxPercentage);
  }
         
  public CatalogDoorOrWindow(String id, String name, String description, 
                             Content icon, Content planIcon, Content model, 
                             float width, float depth, float height, float elevation, boolean movable, 
                             float wallThickness, float wallDistance, Sash [] sashes,
                             float [][] modelRotation, String creator,
                             boolean resizable, boolean deformable, boolean texturable,
                             BigDecimal price, BigDecimal valueAddedTaxPercentage) {
    this(id, name, description, icon, planIcon, model, width, depth, height, elevation, movable,   
        wallThickness, wallDistance, sashes,
        modelRotation, creator, resizable, deformable, texturable, price, valueAddedTaxPercentage, null);
  }
         
  public CatalogDoorOrWindow(String id, String name, String description, 
                             Content icon, Content planIcon, Content model, 
                             float width, float depth, float height, float elevation, boolean movable, 
                             float wallThickness, float wallDistance, Sash [] sashes,
                             float [][] modelRotation, String creator,
                             boolean resizable, boolean deformable, boolean texturable,
                             BigDecimal price, BigDecimal valueAddedTaxPercentage, String currency) {
    this(id, name, description, null, null, null, null, icon, planIcon, model, width, depth, height, elevation, movable, 
        wallThickness, wallDistance, sashes, 
        modelRotation, creator, resizable, deformable, texturable, price, valueAddedTaxPercentage, currency);
  }
         
  public CatalogDoorOrWindow(String id, String name, String description, 
                             String information, String [] tags, Long creationDate, Float grade, 
                             Content icon, Content planIcon, Content model, 
                             float width, float depth, float height, float elevation, boolean movable, 
                             float wallThickness, float wallDistance, Sash [] sashes, 
                             float [][] modelRotation, String creator, 
                             boolean resizable, boolean deformable, boolean texturable, 
                             BigDecimal price, BigDecimal valueAddedTaxPercentage, String currency) {
    this(id, name, description, information, tags, creationDate, grade, 
        icon, planIcon, model, width, depth, height, elevation, movable, 
        null, wallThickness, wallDistance, sashes, 
        modelRotation, creator, resizable, deformable, texturable, price, valueAddedTaxPercentage, currency);
  }
         
  public CatalogDoorOrWindow(String id, String name, String description, 
                             String information, String [] tags, Long creationDate, Float grade, 
                             Content icon, Content planIcon, Content model, 
                             float width, float depth, float height, float elevation, boolean movable, 
                             String cutOutShape, float wallThickness, float wallDistance, Sash [] sashes,
                             float [][] modelRotation, String creator, 
                             boolean resizable, boolean deformable, boolean texturable, 
                             BigDecimal price, BigDecimal valueAddedTaxPercentage, String currency) {
    this(id, name, description, information, tags, creationDate, grade, 
        icon, planIcon, model, width, depth, height, elevation, 1f, movable, 
        cutOutShape, wallThickness, wallDistance, sashes,
        modelRotation, creator, resizable, deformable, texturable, price, valueAddedTaxPercentage, currency);
  }
         
  public CatalogDoorOrWindow(String id, String name, String description, 
                             String information, String [] tags, Long creationDate, Float grade, 
                             Content icon, Content planIcon, Content model, 
                             float width, float depth, float height, float elevation, float dropOnTopElevation, boolean movable, 
                             String cutOutShape, float wallThickness, float wallDistance, Sash [] sashes,
                             float [][] modelRotation, String creator, 
                             boolean resizable, boolean deformable, boolean texturable, 
                             BigDecimal price, BigDecimal valueAddedTaxPercentage, String currency) {
    this(id, name, description, information, tags, creationDate, grade, 
        icon, planIcon, model, width, depth, height, elevation, dropOnTopElevation, movable, 
        cutOutShape, wallThickness, wallDistance, sashes,
        modelRotation, false, creator, resizable, deformable, texturable, price, valueAddedTaxPercentage, currency);  
  }
         
  public CatalogDoorOrWindow(String id, String name, String description, 
                             String information, String [] tags, Long creationDate, Float grade, 
                             Content icon, Content planIcon, Content model, 
                             float width, float depth, float height, float elevation, float dropOnTopElevation, boolean movable, 
                             String cutOutShape, float wallThickness, float wallDistance, Sash [] sashes,
                             float [][] modelRotation, boolean backFaceShown, String creator, 
                             boolean resizable, boolean deformable, boolean texturable, 
                             BigDecimal price, BigDecimal valueAddedTaxPercentage, String currency) {
    super(id, name, description, information, tags, creationDate, grade, 
        icon, planIcon, model, width, depth, height, elevation, dropOnTopElevation, movable, 
        null, modelRotation, backFaceShown, creator, resizable, deformable, texturable, 
        price, valueAddedTaxPercentage, currency);
    this.cutOutShape = cutOutShape;
    this.wallThickness = wallThickness;
    this.wallDistance = wallDistance;
    this.sashes = sashes;
  }
         
  public CatalogDoorOrWindow(String name, Content icon, Content model, 
                             float width, float depth, float height,
                             float elevation, boolean movable, 
                             float wallThickness, float wallDistance, Sash [] sashes, 
                             Integer color, float [][] modelRotation, boolean backFaceShown, 
                             float iconYaw, boolean proportional) {
    super(name, icon, model, width, depth, height, elevation, movable,   
        color, modelRotation, backFaceShown, iconYaw, proportional);
    this.wallThickness = wallThickness;
    this.wallDistance = wallDistance;
    this.sashes = sashes;
    this.cutOutShape = null;
  }

  public float getWallThickness() {
    return this.wallThickness;
  }
  
  public float getWallDistance() {
    return this.wallDistance;
  }
  
  public Sash [] getSashes() {
    if (this.sashes.length == 0) {
      return this.sashes;
    } else {
      return this.sashes.clone();
    }
  }
  
  public String getCutOutShape() {
    return this.cutOutShape;
  }

  public boolean isDoorOrWindow() {
    return true;
  }
}
